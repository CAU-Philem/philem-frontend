package com.philem.philem.data.model

// 상품 1개의 정보를 담는 데이터 클래스
data class ProductItem(
    val id: Int,
    val name: String,
    val price: String, // "1,100,000원" 처럼 텍스트로 처리
    val grade: String, // "A", "B", "C"
    // val imageUrl: String // (나중에 진짜 이미지를 넣을 때 사용)
)