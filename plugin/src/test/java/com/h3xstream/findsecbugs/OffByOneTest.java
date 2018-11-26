package com.h3xstream.findsecbugs;

import com.h3xstream.findbugs.test.BaseDetectorTest;
import com.h3xstream.findbugs.test.EasyBugReporter;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.testng.annotations.Test;

public class OffByOneTest extends BaseDetectorTest {

    @Test
    public void detectOffByONe() throws Exception {
        String[] files = {
                getClassFilePath("testcode/OffByOne")
        };
        EasyBugReporter reporter = spy(new SecurityReporter());
        analyze(files, reporter);

        int lineNumbers[] = {
                 8, 14, 26,
                32, 38, 50
        };

        for (int linenumber : lineNumbers) {
            verify(reporter).doReportBug(
                    bugDefinition()
                            .bugType("OFF_BY_ONE")
                            .inClass("OffByOne").atLine(linenumber)
                            .build()
            );
        }
        verify(reporter, times(lineNumbers.length)).doReportBug(bugDefinition().bugType("OFF_BY_ONE").build());
    }
}
