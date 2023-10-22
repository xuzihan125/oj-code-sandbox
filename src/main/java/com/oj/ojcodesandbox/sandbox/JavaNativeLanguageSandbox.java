package com.oj.ojcodesandbox.sandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRequest;
import com.oj.ojcodesandbox.sandbox.model.ExecuteCodeRespond;
import com.oj.ojcodesandbox.sandbox.model.ExecuteMessage;
import com.oj.ojcodesandbox.sandbox.model.JudgeInfo;
import com.oj.ojcodesandbox.utils.ProcessUtis;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
public class JavaNativeLanguageSandbox implements CodeSandbox{



    private static final String GLOBAL_CODE_DIR_NAME = "tempoCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static void main(String args[]){
        JavaNativeLanguageSandbox tempo = new JavaNativeLanguageSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","2 3"));
        String code = ResourceUtil.readStr("testcode/simpleComputeArgs/unsafe/SimpleCompute.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeRespond executeCodeRespond = tempo.executeCode(executeCodeRequest);
        System.out.println(executeCodeRespond);
    }

    //todo javasecurity manager
    @Override
    public ExecuteCodeRespond executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();
        String language = executeCodeRequest.getLanguage();

        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // check for dir
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        //seperate code from different user
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        //compile
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        ExecuteMessage executeCompileMessage = ProcessUtis.runProcess(compileCmd);
        if(executeCompileMessage.getExitValue()!=0){
            return getErrorRespond(executeCompileMessage.getErrorMessage());
        }
        List<ExecuteMessage> output = new ArrayList<>();
        for(String input : inputList){
            String runCmd = String.format("java -cp %s Main %s", userCodeParentPath, input);
            ExecuteMessage executeMessage = ProcessUtis.runProcess(runCmd);
            output.add(executeMessage);
        }
        // create respond
        ExecuteCodeRespond executeCodeRespond = new ExecuteCodeRespond();
        List<String> outputResult = new ArrayList<>();
        //todo change into more detail information
        Long time = 0L;
        for(ExecuteMessage executeMessage: output){
            if(StrUtil.isNotBlank(executeMessage.getErrorMessage())){
                executeCodeRespond.setMessage(executeMessage.getErrorMessage());
                executeCodeRespond.setStatus(3);
                break;
            }
            time = Math.max(time, executeMessage.getTime());
            outputResult.add(executeMessage.getMessage());
        }
        executeCodeRespond.setOutputList(outputResult);
        executeCodeRespond.setStatus(1);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(time);
        //todo add memory info
//        judgeInfo.setMemory();
        executeCodeRespond.setJudgeInfo(judgeInfo);
        //delete created file
        if (userCodeFile.getParentFile()!=null){
            boolean del = FileUtil.del(userCodeParentPath);
        }
        return executeCodeRespond;
    }

    private ExecuteCodeRespond getErrorRespond(String message){
        ExecuteCodeRespond executeCodeRespond = new ExecuteCodeRespond();
        executeCodeRespond.setOutputList(new ArrayList<>());
        executeCodeRespond.setMessage(message);
        executeCodeRespond.setStatus(2);
        executeCodeRespond.setJudgeInfo(new JudgeInfo());
        return executeCodeRespond;
    }

}
