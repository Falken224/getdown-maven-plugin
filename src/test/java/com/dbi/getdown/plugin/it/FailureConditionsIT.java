package com.dbi.getdown.plugin.it;

import static org.assertj.core.api.Assertions.*;

import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

@MavenJupiterExtension
public class FailureConditionsIT {

    @MavenTest
    public void no_class(MavenExecutionResult result) {
        assertThat(result.isSuccesful()).as("isSuccessful").isFalse();
        assertThat(result.isFailure()).as("isFailure").isTrue();
        assertThat(result.isError()).as("isError").isFalse();
        assertThat(contentOf(result.getMavenLog().getStdout().toFile())).contains("Caused by: java.io.IOException: m.missing_class");
    }

}
