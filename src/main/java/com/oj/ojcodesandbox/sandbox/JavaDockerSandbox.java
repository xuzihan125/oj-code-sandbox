package com.oj.ojcodesandbox.sandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.oj.ojcodesandbox.sandbox.model.ExecuteMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerSandbox extends JavaCodeSandboxTemplate {

    //todo initial of pull image
    public static final boolean firstInit = false;

    private static final Long TIME_OUT = 15 * 1000L;


    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) throws Exception{
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        // create docker container
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String image = "openjdk:8-alpine";
        // pull image
        if (firstInit) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("download image:" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("exception when trying to pull image");
                throw new RuntimeException(e);
            }
        }

        // create container with compiled file inside
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig
                .withMemory(100 * 1024 * 1024L)
                .withMemorySwap(0L)
                .withCpuCount(1L)
                .setBinds(new Bind(userCodeParentPath, new Volume("/app/code")));
        CreateContainerResponse exec = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)
                .exec();
        System.out.println(exec);
        String id = exec.getId();

        //start container
        dockerClient.startContainerCmd(id).exec();
        List<ExecuteMessage> output = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            Long time = 0L;
            String[] args = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app/code", "Main"}, args);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(id)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
//                .withTty(true)
                    .exec();
            System.out.println("create execute command:" + execCreateCmdResponse);

            // execute command and get result
            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {""};
            final String[] errorMessage = {""};
            final boolean[] timeOut = {true};
            String execCreateCmdResponseId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    timeOut[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = errorMessage[0] + new String(frame.getPayload());
                        System.out.println("execute error result:" + errorMessage[0]);
                    } else {
                        message[0] = message[0] + new String(frame.getPayload());
                        System.out.println("execute result:" + message[0]);
                    }

                    super.onNext(frame);
                }
            };
            //get memory usage
            final Long[] maxMemory = {0L};
            StatsCmd statsCmd = dockerClient.statsCmd(id);
            ResultCallback<Statistics> resultCallback = new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    maxMemory[0] = Math.max(maxMemory[0], statistics.getMemoryStats().getUsage());
                    System.out.println("memory usage:" + maxMemory[0]);
                }

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            };
            statsCmd.exec(resultCallback);

            try {
                stopWatch.start();
                dockerClient
                        .execStartCmd(execCreateCmdResponseId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (timeOut[0]) {
                throw new RuntimeException("answer exceed time limit");
            }
//            statsCmd.close();
            executeMessage.setMessage(message[0].trim());
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            output.add(executeMessage);

        }
        return output;
    }
}
