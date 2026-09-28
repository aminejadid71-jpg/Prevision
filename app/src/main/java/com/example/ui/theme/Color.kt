package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Modern White & Crisp Slate/Emerald Theme (No native blue template)
val BrandCharcoal = Color(0xFF0F172A)         // Deep obsidian / slate for primary text and bold actions
val BrandCharcoalDark = Color(0xFF020617)
val BrandSlate = Color(0xFF334155)            // Subtle secondary slate
val BrandSlateLight = Color(0xFFF1F5F9)       // Very soft slate surface

// Fresh modern accent (Agricultural & Payroll Growth)
val BrandAccent = Color(0xFF059669)           // Vibrant Emerald Green
val BrandAccentLight = Color(0xFFECFDF5)      // Soft Mint container
val BrandAccentDark = Color(0xFF047857)

// Backward compatibility mappings with modern white theme values
val BrandPrimary = Color(0xFF0F172A)          // Sleek Charcoal replacing old native blue
val BrandPrimaryDark = Color(0xFF020617)
val BrandPrimaryContainer = Color(0xFFF8FAFC)
val BrandOnPrimaryContainer = Color(0xFF0F172A)

val BrandSecondary = Color(0xFF059669)        // Emerald replacing old bright blue
val BrandSecondaryContainer = Color(0xFFECFDF5)

val BrandSuccess = Color(0xFF10B981)          // Crisp Emerald Success
val BrandSuccessLight = Color(0xFFD1FAE5)
val BrandSuccessDark = Color(0xFF047857)

val BrandWarning = Color(0xFFF59E0B)          // Amber
val BrandWarningLight = Color(0xFFFEF3C7)

val BrandError = Color(0xFFEF4444)            // Coral Red
val BrandErrorLight = Color(0xFFFEE2E2)

// Ultra Clean White Canvas Surfaces
val BackgroundLight = Color(0xFFF8F9FA)       // Modern warm-porcelain off-white background
val SurfaceLight = Color(0xFFFFFFFF)          // Pure White cards and sheets
val SurfaceVariantLight = Color(0xFFF1F5F9)   // Subtle contrast container
val SurfaceBorder = Color(0xFFE2E8F0)         // Refined hairline border
val BorderLight = Color(0xFFE2E8F0)

val TextPrimary = Color(0xFF0F172A)           // Ultra-sharp deep slate
val TextSecondary = Color(0xFF64748B)         // Refined muted slate
val TextTertiary = Color(0xFF94A3B8)

// Minimalist Dark Theme counterparts (if enabled)
val BackgroundDark = Color(0xFF090D16)
val SurfaceDark = Color(0xFF111827)
val SurfaceVariantDark = Color(0xFF1F2937)
val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryDark = Color(0xFF9CA3AF)
val BorderDark = Color(0xFF374151)
