package com.dbi.getdown.plugin.it;

import static org.assertj.core.api.Assertions.*;

import java.io.File;

import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

@MavenJupiterExtension
public class MimimalGetdownTxtIT {

    @MavenTest
    public void minimal(MavenExecutionResult result) {
        assertThat(result.isSuccesful()).as("isSuccessful").isTrue();
        assertThat(result.isFailure()).as("isFailure").isFalse();
        assertThat(result.isError()).as("isError").isFalse();
        File getdowntxt = new File(result.getMavenProjectResult().getBaseDir(), "target/getdown.txt");
        assertThat(getdowntxt).exists().hasSameTextualContentAs(new File(result.getMavenProjectResult().getBaseDir(), "expected/getdown.txt"));
    }
}