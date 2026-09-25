package com.klarfinance.app.domain.model

/** Jalur pengiriman OTP registrasi. [SMS] = Firebase Phone Auth, dipakai kalau WhatsApp (Kirimi)
 * gagal di backend atau nasabah minta kirim ulang via SMS. */
enum class OtpChannel { WHATSAPP, SMS }
