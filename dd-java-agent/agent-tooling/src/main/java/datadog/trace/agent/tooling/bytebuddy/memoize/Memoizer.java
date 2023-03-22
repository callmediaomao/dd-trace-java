package datadog.trace.agent.tooling.bytebuddy.memoize;

import static net.bytebuddy.matcher.ElementMatchers.not;

import datadog.trace.agent.tooling.bytebuddy.TypeInfoCache;
import datadog.trace.agent.tooling.bytebuddy.TypeInfoCache.SharedTypeInfo;
import datadog.trace.agent.tooling.bytebuddy.outline.TypePoolFacade;
import datadog.trace.api.InstrumenterConfig;
import datadog.trace.api.cache.DDCache;
import datadog.trace.api.cache.DDCaches;
import de.thetaphi.forbiddenapis.SuppressForbidden;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressForbidden
@SuppressWarnings("rawtypes")
public final class Memoizer {
  private static final Logger log = LoggerFactory.getLogger(Memoizer.class);

  enum MatcherKind {
    ANNOTATION,
    FIELD,
    METHOD,
    CLASS,
    INTERFACE,
    TYPE // i.e. class or interface
  }

  private static final BitSet NO_MATCH = new BitSet(0);

  private static final int SIZE_HINT = 320; // estimated number of matchers

  // records the kind of matcher and whether matches should be inherited
  private static final BitSet annotationMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet fieldMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet methodMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet classMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet interfaceMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet inheritedMatcherIds = new BitSet(SIZE_HINT);

  // matchers that are ready for memoization
  static final List<ElementMatcher> matchers = new ArrayList<>();

  // small cache to de-duplicate memoization requests
  private static final DDCache<ElementMatcher, MemoizingMatcher> memoizingMatcherCache =
      DDCaches.newFixedSizeIdentityCache(8);

  // caches positive memoized matches
  private static final TypeInfoCache<BitSet> memos =
      new TypeInfoCache<>(InstrumenterConfig.get().getResolverMemoPoolSize(), true);

  // local memoized results, used to detect circular references
  static final ThreadLocal<Map<String, BitSet>> localMemosHolder =
      ThreadLocal.withInitial(HashMap::new);

  private static final int INTERNAL_MATCHERS = 2; // isClass and isConcrete

  // memoize whether the type is a class
  static final ElementMatcher.Junction<TypeDescription> isClass =
      prepare(MatcherKind.CLASS, ElementMatchers.any(), true);

  // memoize whether the type is a concrete class, i.e. no abstract methods
  static final ElementMatcher.Junction<TypeDescription> isConcrete =
      prepare(MatcherKind.CLASS, not(ElementMatchers.isAbstract()), false);

  public static void resetState() {
    // no need to reset the state if we haven't added any external matchers
    if (matchers.size() > INTERNAL_MATCHERS) {
      Memoizer.clear();
    }
  }

  public static void clear() {
    memos.clear();
  }

  static MemoizingMatcher withMatcherId(ElementMatcher matcher) {
    return new MemoizingMatcher(matchers.size());
  }

  /** Prepares a matcher for memoization. */
  static <T> ElementMatcher.Junction<TypeDescription> prepare(
      MatcherKind kind, ElementMatcher.Junction<T> matcher, boolean inherited) {

    MemoizingMatcher memoizingMatcher =
        memoizingMatcherCache.computeIfAbsent(matcher, Memoizer::withMatcherId);

    int matcherId = memoizingMatcher.matcherId;
    if (matcherId < matchers.size()) {
      return memoizingMatcher; // de-duplicated matcher
    } else {
      matchers.add(matcher);
    }

    switch (kind) {
      case ANNOTATION:
        annotationMatcherIds.set(matcherId);
        break;
      case FIELD:
        fieldMatcherIds.set(matcherId);
        break;
      case METHOD:
        methodMatcherIds.set(matcherId);
        break;
      case CLASS:
        classMatcherIds.set(matcherId);
        break;
      case INTERFACE:
        interfaceMatcherIds.set(matcherId);
        break;
      case TYPE:
        classMatcherIds.set(matcherId);
        interfaceMatcherIds.set(matcherId);
        break;
    }

    if (inherited) {
      inheritedMatcherIds.set(matcherId);
    }

    return memoizingMatcher;
  }

  /** Matcher wrapper that triggers memoization on request. */
  static final class MemoizingMatcher
      extends ElementMatcher.Junction.ForNonNullValues<TypeDescription> {
    final int matcherId;

    MemoizingMatcher(int matcherId) {
      this.matcherId = matcherId;
    }

    @Override
    protected boolean doMatch(TypeDescription target) {
      if ("java.lang.Object".equals(target.getName()) || target.isPrimitive()) {
        return false;
      } else {
        return doMemoize(target, localMemosHolder.get()).get(matcherId);
      }
    }
  }

  static BitSet doMemoize(TypeDescription type, Map<String, BitSet> localMemos) {

    String name = type.getName();
    BitSet memo = localMemos.get(name);
    if (null != memo) {
      return memo; // short-circuit circular references
    }

    SharedTypeInfo<BitSet> sharedMemo = memos.find(name);
    if (null != sharedMemo) {
      return sharedMemo.get();
    }

    localMemos.put(name, memo = new BitSet(matchers.size()));
    boolean wasFullParsing = TypePoolFacade.disableFullDescriptions(); // only need outlines here
    try {
      TypeDescription.Generic superType = type.getSuperClass();
      if (null != superType && !"java.lang.Object".equals(superType.getTypeName())) {
        inherit(doMemoize(superType.asErasure(), localMemos), memo);
      }
      for (TypeDescription.Generic intf : type.getInterfaces()) {
        inherit(doMemoize(intf.asErasure(), localMemos), memo);
      }
      for (AnnotationDescription ann : type.getDeclaredAnnotations()) {
        record(annotationMatcherIds, ann.getAnnotationType(), memo);
      }
      for (FieldDescription field : type.getDeclaredFields()) {
        record(fieldMatcherIds, field, memo);
      }
      for (MethodDescription method : type.getDeclaredMethods()) {
        record(methodMatcherIds, method, memo);
      }
      record(type.isInterface() ? interfaceMatcherIds : classMatcherIds, type, memo);
    } catch (Throwable e) {
      if (log.isDebugEnabled()) {
        log.debug(
            "{} recording matches for type {}: {}",
            e.getClass().getSimpleName(),
            name,
            e.getMessage());
      }
    } finally {
      localMemos.remove(name);
      if (wasFullParsing) {
        TypePoolFacade.enableFullDescriptions();
      }
    }

    if (memo.nextSetBit(INTERNAL_MATCHERS) < 0) {
      memo = NO_MATCH; // we're only interested if there's at least one external match
    }

    memos.share(name, null, null, memo);

    return memo;
  }

  /** Inherit positive matches from a super-class or interface. */
  private static void inherit(BitSet superMemo, BitSet memo) {
    int matcherId = superMemo.nextSetBit(0);
    while (matcherId >= 0) {
      if (inheritedMatcherIds.get(matcherId)) {
        memo.set(matcherId);
      }
      matcherId = superMemo.nextSetBit(matcherId + 1);
    }
  }

  /** Run a series of memoized matchers and record positive matches. */
  @SuppressWarnings("unchecked")
  private static void record(BitSet matcherIds, Object type, BitSet memo) {
    int matcherId = matcherIds.nextSetBit(0);
    while (matcherId >= 0) {
      // can skip match if we've already inherited a positive result
      if (!memo.get(matcherId) && matchers.get(matcherId).matches(type)) {
        memo.set(matcherId);
      }
      matcherId = matcherIds.nextSetBit(matcherId + 1);
    }
  }
}
