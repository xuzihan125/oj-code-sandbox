package com.oj.ojcodesandbox.controller;

import cn.hutool.core.io.resource.ResourceUtil;
import com.oj.ojcodesandbox.sandbox.JavaDockerSandbox;
import com.oj.ojcodesandbox.sandbox.JavaNativeCodeSandbox;
import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRequest;
import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRespond;
import com.oj.ojcodesandbox.sandbox.outdated.JavaDockerLanguageSandbox;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@RestController
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);



    @GetMapping("/check")
    public String check(){
        return "running";
    }

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @Resource
    private JavaDockerSandbox javaDockerSandbox;

    /**
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    ExecuteCodeRespond executeCodeDefault(@RequestBody ExecuteCodeRequest executeCodeRequest){
        if(executeCodeRequest == null){
            throw new RuntimeException("empty value is passed");
        }
        logger.info(executeCodeRequest.toString());
        ExecuteCodeRespond executeCodeRespond = javaDockerSandbox.executeCode(executeCodeRequest);
        logger.info(executeCodeRespond.toString());
        return executeCodeRespond;
    }

    /**
     *
     * @param
     * @return
     */
    @GetMapping("/test")
    ExecuteCodeRespond executeCodeTest(){
        JavaDockerLanguageSandbox tempo = new JavaDockerLanguageSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "2 3"));
        String code = ResourceUtil.readStr("testcode/simpleComputeArgs/SimpleCompute.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        return tempo.executeCode(executeCodeRequest);
    }

    /**
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode/native")
    ExecuteCodeRespond executeCodeNative(@RequestBody ExecuteCodeRequest executeCodeRequest){
        if(executeCodeRequest == null){
            throw new RuntimeException("empty value is passed");
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

    /**
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode/docker")
    ExecuteCodeRespond executeCodeDocker(@RequestBody ExecuteCodeRequest executeCodeRequest){
        if(executeCodeRequest == null){
            throw new RuntimeException("empty value is passed");
        }
        return javaDockerSandbox.executeCode(executeCodeRequest);
    }
}
