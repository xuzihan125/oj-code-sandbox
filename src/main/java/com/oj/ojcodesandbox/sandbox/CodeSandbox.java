package com.oj.ojcodesandbox.sandbox;

import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRequest;
import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRespond;

public interface CodeSandbox {

    ExecuteCodeRespond executeCode(ExecuteCodeRequest executeCodeRequest);
}
