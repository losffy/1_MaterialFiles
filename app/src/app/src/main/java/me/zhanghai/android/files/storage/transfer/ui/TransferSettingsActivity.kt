/*
 * Copyright (c) 2025 Manus AI <support@manus.ai>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage.transfer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.TransferSettingsActivityBinding
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.putArgs

/**
 * 定向存储传输设置界面
 */
class TransferSettingsActivity : AppCompatActivity() {

    private lateinit var binding: TransferSettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = TransferSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            val fragment = TransferSettingsFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, TransferSettingsActivity::class.java)
                .putArgs(Args())
        }

        fun newIntent(context: Context, args: Args): Intent {
            return Intent(context, TransferSettingsActivity::class.java)
                .putArgs(args)
        }
    }

    class Args {
        companion object {
            fun fromBundle(bundle: Bundle): Args {
                return Args()
            }
        }
    }
}
