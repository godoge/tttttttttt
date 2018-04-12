package com.likpia.recenthostget.utils;

import android.content.Context;
import android.util.Log;

import com.likpia.recenthostget.SimpleAsyncHandle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by jihai on 2018/4/12.
 */

public class DownloadTool {
    private DownLoadListener loadListener;
    private Context context;
    private String url;
    private File file;
    private boolean isCanceled = false;

    public void cancel() {
        isCanceled = true;
    }

    public void setLoadListener(DownLoadListener loadListener) {
        this.loadListener = loadListener;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public interface DownLoadListener {
        void onSuccess(File file);

        void onFinished();

        void onCanceled();

        void onProgress(int percentage);

        void onStart();

        void onError(String e);
    }

    private DownloadTool(Context context) {

        this.context = context;
    }

    public static class Builder {
        private DownloadTool downloadTool;

        public Builder(Context context) {
            downloadTool = new DownloadTool(context);
        }

        public Builder setLoadListener(DownLoadListener loadListener) {
            downloadTool.setLoadListener(loadListener);
            return this;
        }

        public Builder setFile(File file) {
            downloadTool.setFile(file);
            return this;
        }

        public Builder setUrl(String url) {
            downloadTool.setUrl(url);
            return this;
        }

        public DownloadTool build() {
            return downloadTool;
        }
    }

    public void writeToFile() {
        SimpleAsyncHandle.newBuilder(context).start(new SimpleAsyncHandle.OnGetDataListener() {
            @Override
            public void onPushed(Object object, Context context) {
                int[] longs = (int[]) object;
                int percentage = (int) (((float) longs[0] / (float) longs[1]) * 100f);
                loadListener.onProgress(percentage);
            }

            @Override
            public void onPrepare(Context context) {
                loadListener.onStart();
            }

            @Override
            public int onNetworkHandle(SimpleAsyncHandle.DataBundle bundle) {
                FileOutputStream out = null;
                InputStream in = null;
                try {
                    URL u = new URL(url);
                    URLConnection connection = u.openConnection();
                    connection.setRequestProperty("Accept-Encoding", "identity");
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
                    out = new FileOutputStream(file);
                    in = connection.getInputStream();
                    int totalLength = connection.getContentLength();
                    Log.i("suhaodong_123", totalLength + "");
                    int currentLength = 0;
                    byte[] bytes = new byte[10000];
                    while (true) {
                        if (isCanceled)
                            break;
                        int len = in.read(bytes);
                        if (len == -1)
                            break;
                        out.write(bytes, 0, len);
                        currentLength += len;
                        push(new int[]{currentLength, totalLength});
                    }
                    if (isCanceled) {
                        return 123;
                    } else
                        return SimpleAsyncHandle.ACTION_CODE_SUCCESS;

                } catch (IOException e) {
                    bundle.putString(e.getMessage());
                    return SimpleAsyncHandle.ACTION_CODE_ERROR;
                } finally {
                    try {
                        in.close();
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void onFinished(int actionCode, SimpleAsyncHandle.DataBundle bundle, Context context) {
                if (actionCode == 123) {
                    DownloadTool.this.loadListener.onCanceled();
                } else if (actionCode == SimpleAsyncHandle.ACTION_CODE_SUCCESS) {
                    DownloadTool.this.loadListener.onSuccess(file);
                } else {
                    DownloadTool.this.loadListener.onError(bundle.getString());
                }
                loadListener.onFinished();
            }
        });

    }
}
