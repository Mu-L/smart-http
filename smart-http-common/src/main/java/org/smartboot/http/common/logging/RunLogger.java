/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: RunLogger.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.common.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author 三刀
 * @version V1.0 , 2020/1/1
 */
class RunLogger implements org.smartboot.http.common.logging.Logger {

    private final String loggerName;
    private Logger logger = null;

    RunLogger(String name) {
        this.loggerName = name;
        logger = Logger.getLogger(name);
        init();
    }

    private void init() {
        logger.setUseParentHandlers(false);
        try {
            // 移除已注册的Handler
            Handler[] handlers = logger.getHandlers();
            if (handlers != null) {
                for (Handler h : handlers) {
                    logger.removeHandler(h);
                }
            }

            logger.setLevel(Level.parse(System.getProperty("smarthttp.log.level", "INFO")));


            // 设置控制台日志Handler
            ConsoleHandler ch = new ConsoleHandler();
            ch.setFormatter(new LogFormatter());
            ch.setLevel(Level.ALL);
            try {
                ch.setEncoding("utf8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.addHandler(ch);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log(Level level, String msg, Object... arguments) {
        LogRecord record = new LogRecord(level, null);
        // 处理{}占位符并格式化消息
        record.setMessage(format(msg, arguments));

        if (arguments != null && arguments.length > 0 && arguments[arguments.length - 1] instanceof Throwable) {
            record.setThrown((Throwable) arguments[arguments.length - 1]);
        }
        logger.log(record);
    }

    @Override
    public String getName() {
        return loggerName;
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String msg) {
        log(Level.FINE, msg);
    }

    @Override
    public void trace(String format, Object arg) {
        log(Level.FINE, format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log(Level.FINE, format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        log(Level.FINE, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log(Level.FINE, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String msg) {
        log(Level.CONFIG, msg);
    }

    @Override
    public void debug(String format, Object arg) {
        log(Level.CONFIG, format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log(Level.CONFIG, format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        log(Level.CONFIG, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(Level.CONFIG, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    @Override
    public void info(String format, Object arg) {
        log(Level.INFO, format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log(Level.INFO, format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        log(Level.INFO, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(Level.INFO, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public void warn(String msg) {
        log(Level.WARNING, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log(Level.WARNING, format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log(Level.WARNING, format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log(Level.WARNING, format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(Level.WARNING, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public void error(String msg) {
        log(Level.SEVERE, msg);
    }

    @Override
    public void error(String format, Object arg) {
        log(Level.SEVERE, format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log(Level.SEVERE, format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        log(Level.SEVERE, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(Level.SEVERE, msg, t);
    }

    private String format(String message, Object... params) {
        if (params == null || params.length == 0) {
            return message;
        }

        StringBuilder result = new StringBuilder();
        int start = 0;
        int paramIndex = 0;

        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '{' && i + 1 < message.length() && message.charAt(i + 1) == '}') {
                result.append(message, start, i);
                if (paramIndex < params.length) {
                    result.append(params[paramIndex++]);
                } else {
                    result.append("{}");
                }
                i++;
                start = i + 1;
            }
        }

        if (start < message.length()) {
            result.append(message.substring(start));
        }

        return result.toString();
    }

}
