package com.example.storemobile.data.remote

import com.example.storemobile.data.model.Client
import com.example.storemobile.data.model.LoginRequest
import com.example.storemobile.data.model.LoginResponse
import com.example.storemobile.data.model.NewClientRequest
import com.example.storemobile.data.model.Order
import com.example.storemobile.data.model.OrderRequest
import com.example.storemobile.data.model.Product
import com.example.storemobile.data.model.Seller
import com.example.storemobile.data.model.SyncPushRequest
import com.example.storemobile.data.model.SyncPushResponse
import com.example.storemobile.data.model.SyncSnapshot
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("api/Products")
    suspend fun getProducts(): Response<List<Product>>

    @GET("api/Clients")
    suspend fun getClients(): Response<List<Client>>

    @POST("api/Clients")
    suspend fun createClient(@Body body: NewClientRequest): Response<Client>

    @GET("api/Users/sellers")
    suspend fun getSellers(): Response<List<Seller>>

    // Barcha faol xodimlar (boshliq/admin ham login qila olishi uchun)
    @GET("api/Users")
    suspend fun getStaff(): Response<List<Seller>>

    /* ───────── SINXRON (offline-first, boshliq ilovasi) ───────── */

    // Ertalab WiFi'da — barcha ma'lumotni yuklab olish
    @GET("api/Sync/snapshot")
    suspend fun getSnapshot(): Response<SyncSnapshot>

    // Do'konga qaytib — offline amallarni yuborish + eng yangi snapshotni olish
    @POST("api/Sync/push")
    suspend fun pushSync(@Body body: SyncPushRequest): Response<SyncPushResponse>

    @POST("api/Users/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/Orders")
    suspend fun createOrder(@Body body: OrderRequest): Response<Order>

    @GET("api/Orders/pending")
    suspend fun getPendingOrders(): Response<List<Order>>

    @GET("api/Orders/history")
    suspend fun getHistory(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<Order>>
}