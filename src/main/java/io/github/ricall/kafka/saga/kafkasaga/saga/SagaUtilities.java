/*
 * Copyright (c) 2021 Richard Allwood
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.ricall.kafka.saga.kafkasaga.saga;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.util.Random;

@UtilityClass
public class SagaUtilities {

    private static final Random RANDOM = new Random();

    @SneakyThrows
    public static void delay(long timeInMilliseconds) {
        Thread.sleep(timeInMilliseconds);
    }

    public static void delayWithRandom(long timeInMillseconds, int maxRandomTime) {
        delay(timeInMillseconds + RANDOM.nextInt(maxRandomTime));
    }

    public static boolean randomFailure(int percentageChance) {
        return RANDOM.nextInt(100) < percentageChance;
    }

}
