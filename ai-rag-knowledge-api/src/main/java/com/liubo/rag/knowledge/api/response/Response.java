package com.liubo.rag.knowledge.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author 68
 * 2026/1/31 15:26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 2910928743848553849L;
    private Integer code;
    private String info;
    private T data;
}
