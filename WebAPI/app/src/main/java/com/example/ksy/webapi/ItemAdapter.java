package com.example.ksy.webapi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder>{
    private List<APIInfo> itemList;
    static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView cover;
        TextView play,title,content,duration,videoReview,createTime;
        ProgressBar progressBar;
        SeekBar seekBar;
        List<Bitmap> priviewImage;

        public ViewHolder(View view){
            super(view);
            cover = (ImageView)view.findViewById(R.id.picture);
            play = (TextView)view.findViewById(R.id.play);
            title = (TextView)view.findViewById(R.id.title);
            content = (TextView)view.findViewById(R.id.content);
            duration = (TextView)view.findViewById(R.id.duration);
            createTime = (TextView)view.findViewById(R.id.create_time);
            videoReview = (TextView)view.findViewById(R.id.video_review);
            progressBar = (ProgressBar)view.findViewById(R.id.progress_bar);
            seekBar = (SeekBar)view.findViewById(R.id.seekbar);
            priviewImage = new ArrayList<>();
            //seekBar.setEnabled(false);
        }
    }

    public ItemAdapter(List<APIInfo> itemList){
        this.itemList = itemList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent ,int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder,int position){
        final APIInfo item = itemList.get(position);
        final ViewHolder viewHolder = holder;
        //holder.cover.setImageURI(Uri.parse(item.GetCoverURL()));
        holder.play.setText(String.valueOf(item.GetPlay()));
        holder.title.setText(item.GetTitle());
        holder.content.setText(item.GetContent());
        holder.duration.setText(item.GetDuration());
        holder.createTime.setText(item.GetCreateTime());
        holder.videoReview.setText(String.valueOf(item.GetVideoReview()));

        Observable.create(new ObservableOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Bitmap> emitter)throws Exception{
                try{
                    URL url = new URL(item.GetCoverURL());
                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    emitter.onNext(bitmap);
                }catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Observer<Bitmap>() {
              @Override
              public void onSubscribe(Disposable d) {
              }

              @Override
              public void onNext(Bitmap bitmap) {
                    viewHolder.progressBar.setVisibility(View.GONE);
                    //viewHolder.cover.setVisibility(View.VISIBLE);
                    viewHolder.cover.setImageBitmap(bitmap);
              }

              @Override
              public void onError(Throwable e) {
              }
              @Override
              public void onComplete() {
              }
          });

        Observable.create(new ObservableOnSubscribe<PriviewInfo>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<PriviewInfo> emitter)throws Exception{
                HttpURLConnection connection = null;
                try{
                    URL url = new URL("https://api.bilibili.com/pvideo?aid="+item.GetAid());
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setReadTimeout(5000);
                    connection.setConnectTimeout(5000);
                    if(connection.getResponseCode() == 200){
                        InputStream in = connection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while((line = reader.readLine()) != null){
                            response.append(line);
                        }
                        JSONObject jsonObject = new JSONObject(response.toString());
                        if(jsonObject.getInt("code") == 0){
                            Gson gson = new Gson();
                            PriviewInfo priviewInfo = gson.fromJson(jsonObject.getString("data"),PriviewInfo.class);
                            emitter.onNext(priviewInfo);
                        }else{
                            Log.d("ItemAdapter","该 aid 不正确");
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(connection != null){
                        connection.disconnect();
                    }
                }
            }

        }).subscribeOn(Schedulers.newThread())
            .observeOn(Schedulers.newThread())
            .subscribe(new Observer<PriviewInfo>() {
                  @Override
                  public void onSubscribe(Disposable d) {
                  }

                  @Override
                  public void onNext(PriviewInfo priviewInfo) {
                      try{
                          URL url = new URL(priviewInfo.GetImage().get(0));
                          HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                          InputStream inputStream = connection.getInputStream();
                          Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                          int pictureNum = priviewInfo.GetIndex().size()-2;
                          int xLen = priviewInfo.GetImageXLen();
                          int imageXSize = priviewInfo.GetImageXSize();
                          int imageYSize = priviewInfo.GetImageYSize();
                          int yLen = pictureNum / xLen +1;
                          for(int y = 0;y < yLen;y++){
                              //for(int x = 0;)
                          }

                      }catch (Exception e) {
                          e.printStackTrace();
                      }

                  }

                  @Override
                  public void onError(Throwable e) {

                  }

                  @Override
                  public void onComplete() {

                  }
          });
    }

    @Override
    public int getItemCount(){
        return itemList.size();
    }
}
