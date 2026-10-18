package com.agritech.exception;public class ApiException extends RuntimeException{public ApiException(String m){super(m);}public ApiException(String m,Throwable c){super(m,c);}}
