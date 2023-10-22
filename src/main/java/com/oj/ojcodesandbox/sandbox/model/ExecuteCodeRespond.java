package com.oj.ojcodesandbox.sandbox.model;

import com.oj.ojcodesandbox.sandbox.model.JudgeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRespond {
    private List<String> outputList;

    private String message;

    private Integer status;

    private JudgeInfo judgeInfo;

}
