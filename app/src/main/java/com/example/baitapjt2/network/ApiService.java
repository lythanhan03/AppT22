package com.example.baitapjt2.network;

import com.example.baitapjt2.model.Post;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {  // Đổi từ class thành interface
    @GET("posts")
    Call<List<Post>> getPosts();
}
