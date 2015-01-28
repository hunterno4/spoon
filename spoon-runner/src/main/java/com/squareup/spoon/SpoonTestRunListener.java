package com.squareup.spoon;

import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.squareup.spoon.adapters.TestIdentifierAdapter;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.spoon.SpoonLogger.logDebug;
import static com.squareup.spoon.SpoonLogger.logError;

/** Marshals an {@link ITestRunListener}'s output to a {@link DeviceResult.Builder}. */
final class SpoonTestRunListener implements ITestRunListener {
  private final DeviceResult.Builder result;
  private final Map<TestIdentifier, DeviceTestResult.Builder> methodResults =
      new HashMap<TestIdentifier, DeviceTestResult.Builder>();
  private final boolean debug;
  private final TestIdentifierAdapter testIdentifierAdapter;

  SpoonTestRunListener(DeviceResult.Builder result, boolean debug,
      TestIdentifierAdapter testIdentifierAdapter) {
    checkNotNull(result);
    this.result = result;
    this.debug = debug;
    this.testIdentifierAdapter = testIdentifierAdapter;
  }

  @Override public void testRunStarted(String runName, int testCount) {
    logDebug(debug, "testCount=%d runName=%s", testCount, runName);
    result.startTests();
  }

  @Override public void testStarted(TestIdentifier test) {
    logDebug(debug, "test=%s", test);
    DeviceTestResult.Builder methodResult = new DeviceTestResult.Builder().startTest();
    methodResults.put(testIdentifierAdapter.adapt(test), methodResult);
  }

  @Override public void testFailed(TestFailure status, TestIdentifier test, String trace) {
    logDebug(debug, "test=%s", test);
    test = testIdentifierAdapter.adapt(test);
    DeviceTestResult.Builder methodResult = methodResults.get(test);
    if (methodResult == null) {
      logError("unknown test=%s", test);
      methodResult = new DeviceTestResult.Builder();
      methodResults.put(test, methodResult);
    }
    switch (status) {
      case FAILURE:
        logDebug(debug, "failed %s", trace);
        methodResult.markTestAsFailed(trace);
        break;
      case ERROR:
        logDebug(debug, "error %s", trace);
        methodResult.markTestAsError(trace);
        break;
      default:
        throw new IllegalArgumentException("Unknown test failure status: " + status);
    }
  }

  @Override public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
    logDebug(debug, "test=%s", test);
    test = testIdentifierAdapter.adapt(test);
    DeviceTestResult.Builder methodResult = methodResults.get(test);
    if (methodResult == null) {
      logError("unknown test=%s", test);
      methodResult = new DeviceTestResult.Builder().startTest();
      methodResults.put(test, methodResult);
    }
    DeviceTestResult.Builder methodResultBuilder = methodResult.endTest();
    result.addTestResultBuilder(DeviceTest.from(test), methodResultBuilder);
  }

  @Override public void testRunFailed(String errorMessage) {
    logDebug(debug, "errorMessage=%s", errorMessage);
    result.addException(errorMessage);
  }

  @Override public void testRunStopped(long elapsedTime) {
    logDebug(debug, "elapsedTime=%d", elapsedTime);
  }

  @Override public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
    logDebug(debug, "elapsedTime=%d", elapsedTime);
    result.endTests();
  }
}
