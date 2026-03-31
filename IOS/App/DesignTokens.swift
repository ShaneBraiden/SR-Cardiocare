// DesignTokens.swift
// Auto-generated from Google Stitch Project 14107272513072708956
// DO NOT hardcode any design values anywhere else in the codebase.
// All UI elements must reference these tokens exclusively.
//
// Source screens: 29 screens fetched via Stitch MCP on 2026-03-01
// Theme: Light mode primary, Inter/Lexend fonts, ROUND_EIGHT, saturation 2

import UIKit
import SwiftUI

// MARK: - DesignTokens

struct DesignTokens {

    // MARK: - Colors

    struct Colors {
        // Primary Brand — Greyish teal/green
        static let primary = UIColor(hex: "#5A9EA6")       // Desaturated teal
        static let primaryDark = UIColor(hex: "#4A8A91")   // Darker greyish teal
        static let primaryLight = UIColor(hex: "#C5DFE2") // Light greyish teal

        // Backgrounds — adaptive with grey undertones
        static let backgroundLight = UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: "#1A1F21") : UIColor(hex: "#F3F5F5") }
        static let backgroundDark = UIColor(hex: "#1A1F21")

        // Surfaces — adaptive with grey undertones
        static let surfaceLight = UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: "#252B2D") : UIColor(hex: "#FAFBFB") }
        static let surfaceDark = UIColor(hex: "#252B2D")

        // Text — adaptive
        static let textMain = UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: "#E8ECEE") : UIColor(hex: "#1A1F21") }
        static let textSub = UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: "#8A9499") : UIColor(hex: "#5A6670") }
        static let textSecondary = UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: "#8A9499") : UIColor(hex: "#5A7678") }

        // Neutrals — adaptive with grey tones
        static let neutralLight = UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: "#3A4245") : UIColor(hex: "#DDE2E3") }
        static let neutralGrey = UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: "#4A5458") : UIColor(hex: "#D5DBDD") }
        static let neutralDark = UIColor(hex: "#8A9499")

        // Special — adaptive with grey undertones
        static let bubbleGrey = UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: "#252B2D") : UIColor(hex: "#E8ECED") }
        static let chartTeal = UIColor(hex: "#5A9EA6")     // Matches primary
        static let chartLightTeal = UIColor(hex: "#D5EAEC")
        static let chartSecondaryTeal = UIColor(hex: "#8ABFC5")
        static let chartGrey = UIColor(hex: "#C5CDD2")

        // Semantic
        static let error = UIColor(hex: "#EF4444")
        static let success = UIColor(hex: "#10B981")
        static let warning = UIColor(hex: "#F59E0B")

        // Slate scale (from Tailwind — used in Stitch screens)
        static let slate100 = UIColor(hex: "#F1F5F9")
        static let slate200 = UIColor(hex: "#E2E8F0")
        static let slate300 = UIColor(hex: "#CBD5E1")
        static let slate400 = UIColor(hex: "#94A3B8")
        static let slate500 = UIColor(hex: "#64748B")
        static let slate600 = UIColor(hex: "#475569")
        static let slate700 = UIColor(hex: "#334155")
        static let slate800 = UIColor(hex: "#1E293B")
        static let slate900 = UIColor(hex: "#0F172A")

        // MARK: SwiftUI Colors

        struct SwiftUI {
            static let primary = Color(Colors.primary)
            static let primaryDark = Color(Colors.primaryDark)
            static let primaryLight = Color(Colors.primaryLight)
            static let backgroundLight = Color(Colors.backgroundLight)
            static let backgroundDark = Color(Colors.backgroundDark)
            static let surfaceLight = Color(Colors.surfaceLight)
            static let surfaceDark = Color(Colors.surfaceDark)
            static let textMain = Color(Colors.textMain)
            static let textSub = Color(Colors.textSub)
            static let textSecondary = Color(Colors.textSecondary)
            static let neutralLight = Color(Colors.neutralLight)
            static let neutralGrey = Color(Colors.neutralGrey)
            static let neutralDark = Color(Colors.neutralDark)
            static let bubbleGrey = Color(Colors.bubbleGrey)
            static let chartTeal = Color(Colors.chartTeal)
            static let chartLightTeal = Color(Colors.chartLightTeal)
            static let chartSecondaryTeal = Color(Colors.chartSecondaryTeal)
            static let chartGrey = Color(Colors.chartGrey)
            static let error = Color(Colors.error)
            static let success = Color(Colors.success)
            static let warning = Color(Colors.warning)
        }
    }

    // MARK: - Typography

    struct Typography {
        // Font Families
        static let primaryFamily = "Inter"
        static let accentFamily = "Lexend"
        static let monoFamily = "Roboto"

        // Font Sizes (CGFloat)
        static let caption2: CGFloat = 10
        static let caption: CGFloat = 12
        static let footnote: CGFloat = 13
        static let subheadline: CGFloat = 14
        static let body: CGFloat = 16
        static let callout: CGFloat = 15
        static let headline: CGFloat = 17
        static let title3: CGFloat = 20
        static let title2: CGFloat = 22
        static let title1: CGFloat = 24
        static let title: CGFloat = 24  // alias for title1
        static let largeTitle: CGFloat = 28
        static let hero: CGFloat = 32
        static let display: CGFloat = 36

        // Font Weights
        struct Weight {
            static let light: UIFont.Weight = .light          // 300
            static let regular: UIFont.Weight = .regular      // 400
            static let medium: UIFont.Weight = .medium        // 500
            static let semibold: UIFont.Weight = .semibold    // 600
            static let bold: UIFont.Weight = .bold            // 700
        }

        // Pre-built fonts
        static func inter(_ size: CGFloat, weight: UIFont.Weight = .regular) -> UIFont {
            let descriptor = UIFontDescriptor(name: primaryFamily, size: size)
            return UIFont(descriptor: descriptor, size: size)
        }

        static func lexend(_ size: CGFloat, weight: UIFont.Weight = .regular) -> UIFont {
            let descriptor = UIFontDescriptor(name: accentFamily, size: size)
            return UIFont(descriptor: descriptor, size: size)
        }
    }

    // MARK: - Spacing

    struct Spacing {
        static let xxxs: CGFloat = 2
        static let xxs: CGFloat = 4
        static let xs: CGFloat = 6
        static let sm: CGFloat = 8
        static let md: CGFloat = 12
        static let base: CGFloat = 16
        static let lg: CGFloat = 20
        static let xl: CGFloat = 24
        static let xxl: CGFloat = 32
        static let xxxl: CGFloat = 48
        static let xxxxl: CGFloat = 64

        // Screen edge padding (consistent across all screens)
        static let screenHorizontal: CGFloat = 16
        static let screenVertical: CGFloat = 16
        static let cardPadding: CGFloat = 16
        static let sectionSpacing: CGFloat = 24
        static let listItemSpacing: CGFloat = 12
    }

    // MARK: - Radius

    struct Radius {
        static let none: CGFloat = 0
        static let sm: CGFloat = 4      // 0.25rem
        static let base: CGFloat = 8    // 0.5rem — DEFAULT (ROUND_EIGHT theme)
        static let lg: CGFloat = 12     // 0.75rem
        static let xl: CGFloat = 16     // 1rem
        static let xxl: CGFloat = 24    // 1.5rem
        static let xxxl: CGFloat = 32   // 2rem
        static let full: CGFloat = 9999 // Pill / circle

        // Semantic aliases
        static let button: CGFloat = xl
        static let card: CGFloat = xl
        static let input: CGFloat = base
        static let chip: CGFloat = full
        static let avatar: CGFloat = full
        static let bottomSheet: CGFloat = xxl
    }

    // MARK: - Shadows

    struct Shadow {
        static let sm = ShadowStyle(color: UIColor.black.withAlphaComponent(0.05), radius: 2, x: 0, y: 1)
        static let md = ShadowStyle(color: UIColor.black.withAlphaComponent(0.1), radius: 4, x: 0, y: 2)
        static let lg = ShadowStyle(color: UIColor.black.withAlphaComponent(0.1), radius: 8, x: 0, y: 4)
        static let xl = ShadowStyle(color: UIColor.black.withAlphaComponent(0.15), radius: 16, x: 0, y: 8)
        static let primaryGlow = ShadowStyle(color: Colors.primary.withAlphaComponent(0.3), radius: 12, x: 0, y: 4)
    }

    // MARK: - Animation

    struct Animation {
        static let fast: TimeInterval = 0.15
        static let normal: TimeInterval = 0.3
        static let slow: TimeInterval = 0.5
    }
}

// MARK: - Supporting Types

struct ShadowStyle {
    let color: UIColor
    let radius: CGFloat
    let x: CGFloat
    let y: CGFloat
}

// MARK: - UIColor Hex Extension

extension UIColor {
    convenience init(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0
        Scanner(string: hexSanitized).scanHexInt64(&rgb)

        let length = hexSanitized.count
        let r, g, b, a: CGFloat

        switch length {
        case 6:
            r = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
            g = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
            b = CGFloat(rgb & 0x0000FF) / 255.0
            a = 1.0
        case 8:
            r = CGFloat((rgb & 0xFF000000) >> 24) / 255.0
            g = CGFloat((rgb & 0x00FF0000) >> 16) / 255.0
            b = CGFloat((rgb & 0x0000FF00) >> 8) / 255.0
            a = CGFloat(rgb & 0x000000FF) / 255.0
        default:
            r = 0; g = 0; b = 0; a = 1.0
        }

        self.init(red: r, green: g, blue: b, alpha: a)
    }
}

// MARK: - Color Hex Extension (SwiftUI)

extension Color {
    init(hex: String) {
        self.init(UIColor(hex: hex))
    }
}
