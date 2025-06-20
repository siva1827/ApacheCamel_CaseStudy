package com.UST.Apache_Camel.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class StockUpdateDetails implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer soldOut;
    private Integer damaged;
}