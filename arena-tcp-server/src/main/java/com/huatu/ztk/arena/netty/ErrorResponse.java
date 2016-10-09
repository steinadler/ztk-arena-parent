package com.huatu.ztk.arena.netty;

/**
 * Created by shaojieyue
 * Created time 2016-10-08 14:51
 */
public class ErrorResponse extends Response {

    public static final ErrorResponse INVALID_PARAM = new ErrorResponse(100001,"非法的参数");
    public static final ErrorResponse UNKNOW_ACTION = new ErrorResponse(100002,"未知的请求");
    public static final ErrorResponse AUTHENTICATION_FAIL = new ErrorResponse(100003,"身份验证失败");
    public static final ErrorResponse JOIN_GAME_FAIL = new ErrorResponse(100004,"加入游戏失败");
    public static final ErrorResponse LEAVE_GAME_FAIL = new ErrorResponse(100005,"离开游戏失败");

    private ErrorResponse(int code, String message) {
        this.code=code;
        this.message=message;
    }

}
