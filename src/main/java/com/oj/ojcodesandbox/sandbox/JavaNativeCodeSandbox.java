package com.oj.ojcodesandbox.sandbox;

import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRequest;
import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRespond;
import org.springframework.stereotype.Component;


/**
 * java native sandbox implements
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeRespond executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
