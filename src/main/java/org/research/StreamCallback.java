package org.research;

public interface StreamCallback {
    void onResponsePart(String part);
    void onComplete();
    void onError(String errorMessage);
}
