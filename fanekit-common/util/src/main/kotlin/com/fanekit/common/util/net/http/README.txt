// 简单 GET 请求
val response1 = httpClient.get("https://api.github.com/users/octocat")
println("Success: ${response1.success}")
println("Status: ${response1.code}")
println("Body: ${response1.bodyString}")

// 带查询参数的 GET 请求
val queries = mapOf(
    "page" to "1",
    "per_page" to "10"
)
val headers = mapOf(
    "Accept" to "application/json",
    "User-Agent" to "MyApp/1.0"
)
val response2 = httpClient.get("https://api.github.com/search/repositories", queries, headers)

// 异步 GET 请求
httpClient.getAsync("https://jsonplaceholder.typicode.com/posts/1") { response, error ->
    if (response?.success == true) {
        println("Async GET Success: ${response.bodyString}")
    } else {
        println("Async GET Failed: ${response?.errorMessage ?: error?.message}")
    }
}

// POST JSON 请求
val jsonBody = """
    {
        "title": "测试标题",
        "body": "测试内容",
        "userId": 1
    }
""".trimIndent()

val response3 = httpClient.postJson(
    url = "https://jsonplaceholder.typicode.com/posts",
    jsonBody = jsonBody,
    headers = mapOf("Content-Type" to "application/json")
)

// POST 表单请求
val formData = mapOf(
    "username" to "testuser",
    "password" to "testpass",
    "email" to "test@example.com"
)

val response4 = httpClient.postForm(
    url = "https://httpbin.org/post",
    formParams = formData
)

// POST Multipart 请求（文件上传）
val textFields = mapOf("description" to "测试文件上传")
val files = mapOf("file" to File("test.txt"))

val response5 = httpClient.postMultipart(
    url = "https://httpbin.org/post",
    formFields = textFields,
    fileParams = files
)

// 异步 POST JSON 请求
httpClient.postJsonAsync(
    url = "https://jsonplaceholder.typicode.com/posts",
    jsonBody = jsonBody
) { response, error ->
    if (response?.success == true) {
        println("Async POST Success: ${response.code}")
    } else {
        println("Async POST Failed: ${response?.errorMessage}")
    }
}


// PUT JSON 请求
val updateJson = """
    {
        "id": 1,
        "title": "更新后的标题",
        "body": "更新后的内容",
        "userId": 1
    }
""".trimIndent()

val response6 = httpClient.putJson(
    url = "https://jsonplaceholder.typicode.com/posts/1",
    jsonBody = updateJson
)

// 异步 PUT 请求
httpClient.putJsonAsync(
    url = "https://jsonplaceholder.typicode.com/posts/1",
    jsonBody = updateJson
) { response, error ->
    println("PUT Result: ${response?.success}")
}


// DELETE 请求
val response7 = httpClient.delete("https://jsonplaceholder.typicode.com/posts/1")

// 带查询参数的 DELETE 请求
val deleteQueries = mapOf("force" to "true")
val response8 = httpClient.delete(
    url = "https://api.example.com/items/123",
    queries = deleteQueries,
    headers = mapOf("Authorization" to "Bearer your-token")
)

// 异步 DELETE 请求
httpClient.deleteAsync("https://jsonplaceholder.typicode.com/posts/1") { response, error ->
    if (response?.success == true) {
        println("Delete Success: ${response.code}")
    } else {
        println("Delete Failed: ${response?.errorMessage}")
    }
}


// 同步下载文件
val downloadFile = File("downloaded_image.jpg")
val downloadResponse = httpClient.downloadFile(
    url = "https://picsum.photos/200/300",
    saveToFile = downloadFile
)

if (downloadResponse.success) {
    println("文件下载成功: ${downloadFile.absolutePath}")
    println("文件大小: ${downloadFile.length()} bytes")
} else {
    println("下载失败: ${downloadResponse.errorMessage}")
}

// 异步下载文件
val asyncDownloadFile = File("async_downloaded.pdf")
httpClient.downloadFileAsync(
    url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
    saveToFile = asyncDownloadFile
) { response, error ->
    if (response?.success == true) {
        println("异步下载成功: ${asyncDownloadFile.absolutePath}")
    } else {
        println("异步下载失败: ${response?.errorMessage ?: error?.message}")
    }
}