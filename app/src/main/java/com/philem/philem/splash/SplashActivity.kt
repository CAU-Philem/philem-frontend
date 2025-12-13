package com.philem.philem.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.philem.philem.search.SearchActivity
import com.philem.philem.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2초 후에 SearchActivity로 이동하는 코드
        Handler(Looper.getMainLooper()).postDelayed({
            // SearchActivity로 넘어갈 Intent(의도) 생성
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)

            // finish()를 호출해서 스플래시 화면(MainActivity)을 종료합니다.
            // (뒤로가기 버튼을 눌렀을 때 다시 뜨지 않도록)
            finish()
        }, 2000) // 2000 밀리초 = 2초
    }
}