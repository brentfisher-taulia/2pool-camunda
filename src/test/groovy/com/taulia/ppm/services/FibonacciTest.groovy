package com.taulia.ppm.services

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.junit.Assert.assertEquals

@RunWith(Parameterized)
class FibonacciTest {

  @Parameterized.Parameters
   static Object[][] data() {
    [[0,0],[1,1],[2,1]]

  }

  private int fInput;

  private int fExpected;

  public FibonacciTest(int input, int expected) {
    fInput = input;
    fExpected = expected;
  }

  @Test
  public void test() {
    assertEquals(fExpected, Fibonacci.compute(fInput));
  }


}
