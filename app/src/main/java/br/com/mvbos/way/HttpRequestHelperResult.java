package br.com.mvbos.way;

/**
 * Created by Marcus Becker on 13/08/2016.
 */
public interface HttpRequestHelperResult {
    public void recieveResult(int id, StringBuilder response, Object extraData, Exception error);
}
