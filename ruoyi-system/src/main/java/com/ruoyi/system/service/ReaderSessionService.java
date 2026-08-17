package com.ruoyi.system.service;

public interface ReaderSessionService
{
    String create(String cardNo);

    String resolve(String token);

    void remove(String token);
}
