package com.zyj.filepreferences.lib.cache;

import android.content.Context;
import android.text.TextUtils;
import com.zyj.filepreferences.lib.disklrucache.DiskLruCache;
import com.zyj.filepreferences.lib.util.CacheUtil;
import com.zyj.filepreferences.lib.util.LogUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by ${zyj} on 2016/9/2.
 */
public class DiskCacheManager {

    private static DiskCacheManager mdiskCacheManager;
    private DiskCache mdiskCache ;

    private DiskCacheManager( Context context ){
        if ( mdiskCache == null ){
            mdiskCache = new ExternalSDCardCacheDiskCacheFactory( context ) ;
        }
    }

    public static DiskCacheManager getInstance( Context context ){
        if ( mdiskCacheManager == null ){
            mdiskCacheManager = new DiskCacheManager( context  ) ;
        }
        return mdiskCacheManager ;
    }

    public void setDiskCache( DiskCache diskCache ){
        if ( diskCache != null ){
            mdiskCache = diskCache ;
        }
    }

    public String read(String key ){
        if (TextUtils.isEmpty( key )){
            LogUtil.d( "filePreferences: read , the key ==  null" );
            return null ;
        }

        InputStream inputStream = getInputStream( key ) ;
        if ( inputStream == null ) {
            return null;
        }

        LogUtil.d( "filePreferences: isReading, key: " + key );

        String result = "" ;
        ByteArrayOutputStream baos = null ;
        try {
            baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buf = new byte[128];

            while ((len = inputStream.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            baos.flush();
            result = baos.toString() ;

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if ( inputStream != null ){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if ( baos != null ){
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        LogUtil.d( "filePreferences: read complete , key: " + key +  "   value: " + result );
        return  result ;
    }

    public Boolean write( String key , String value){
        if (TextUtils.isEmpty( key )){
            LogUtil.d( "filePreferences: write , the key ==  null" );
            return false ;
        }

        DiskLruCache diskLruCache = mdiskCache.getDiskLruCache() ;
        if ( diskLruCache == null ) {
            LogUtil.d( "filePreferences: write , diskLruCache == null" );
            return false ;
        }

        String md5Key = CacheUtil.getKey( key );

        DiskLruCache.Editor editor = null ;
        InputStream inputStream = null ;
        OutputStream outputStream = null ;
        BufferedOutputStream bufferedOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            editor = diskLruCache.edit( md5Key );
            if ( editor != null ){
                outputStream = editor.newOutputStream( 0 ) ;
                byte bytes[]= null ;
                bytes= value.getBytes( "UTF-8"); //新加的
                outputStream.write( bytes );
                editor.commit();
                LogUtil.d( "filePreferences : 缓存写入成功");
                return  true ;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if ( editor != null ){
                try {
                    editor.abort();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            LogUtil.d( "filePreferences : 缓存写入失败");
        }finally {
            try {
                diskLruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if ( inputStream != null ){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if ( outputStream != null ){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if ( bufferedInputStream != null ){
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if ( bufferedOutputStream != null ){
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false ;
    }

    private InputStream getInputStream( String key ){
        DiskLruCache diskLruCache = mdiskCache.getDiskLruCache() ;
        if ( diskLruCache == null ) {
            LogUtil.d( "filePreferences: getInputStream , diskLruCache == null" );
            return null ;
        }
        try {
            String md5Key = CacheUtil.getKey( key );
            DiskLruCache.Snapshot snapShot = diskLruCache.get(md5Key);
            if ( snapShot != null ){
                return snapShot.getInputStream(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null ;
        }
        return null ;
    }

    private Outer getOuter( String key ){
        DiskLruCache diskLruCache = mdiskCache.getDiskLruCache() ;
        if ( diskLruCache == null ) {
            LogUtil.d( "filePreferences: getOutputStream , diskLruCache == null" );
            return null ;
        }

        String md5Key = CacheUtil.getKey( key );
        DiskLruCache.Editor editor = null;
        try {
            editor = diskLruCache.edit( md5Key );
            if ( editor != null ){
                Outer outer = new Outer() ;
                outer.editor = editor ;
                outer.outputStream = editor.newOutputStream( 0 ) ;
                return outer ;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null ;
        }
        return null ;
    }

    private byte[] getBytes(String key){
        byte[] res = null;
        InputStream is = getInputStream( key ) ;
        if ( is == null) return null ;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[256];
            int len = 0;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            res = baos.toByteArray();
            return res ;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if ( is != null ){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if ( baos != null ){
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return res;
    }


    public void cleanCache(){
        mdiskCache.clearCache();
    }

    public void removeCache( String key ){
        mdiskCache.removeCache(  CacheUtil.getKey( key ) );
    }

    /**
     * get diskCache size
     * @return
     */
    public long getDiskCacheSize(){
        return mdiskCache.getTotalCacheSize() ;
    }



}
