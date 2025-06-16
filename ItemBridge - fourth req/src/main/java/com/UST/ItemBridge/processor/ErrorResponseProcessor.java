package com.UST.ItemBridge.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ErrorResponseProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
        exchange.getIn().setBody(new ErrorResponse("ERROR", errorMessage));
    }

    private static class ErrorResponse {
        private String status;
        private String message;

        public ErrorResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}