package com.netazoic.jxapi;

import org.junit.Test;

import com.rusticisoftware.tincan.RemoteLRS;
import com.rusticisoftware.tincan.Statement;

public class TestAcvitityProfile {

    @Test
    public void testSaveStatement() throws Exception {
        RemoteLRS obj = getLRS();

        Statement st = new Statement();
        st.stamp(); // triggers a PUT
        st.setActor(mockAgent());
        st.setVerb(mockVerbDisplay());
        st.setObject(mockActivity("testSaveStatement"));

        obj.saveStatement(st);
    }

}
