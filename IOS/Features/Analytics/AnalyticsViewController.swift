// AnalyticsViewController.swift
// SR-Cardiocare — Patient Progress Analytics
// Stitch Screen: 49ed11ea (08_analytics)
// Shows: weekly bar chart, stat cards (compliance, pain trend, streak), time filter

import UIKit

final class AnalyticsViewController: UIViewController {

    // MARK: - Data

    private let weeklyData: [(day: String, activity: CGFloat, painDot: CGFloat)] = [
        ("M", 0.6, 0.8), ("T", 0.4, 0.75), ("W", 0.8, 0.5),
        ("T", 0.5, 0.6), ("F", 0.9, 0.3), ("S", 0.3, 0.4), ("S", 0.2, 0.35),
    ]

    // MARK: - UI

    private let scrollView: UIScrollView = {
        let sv = UIScrollView()
        sv.showsVerticalScrollIndicator = false
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    private let contentStack: UIStackView = {
        let s = UIStackView()
        s.axis = .vertical
        s.spacing = DesignTokens.Spacing.xl
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    // Header
    private let titleLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("analytics_title", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.title, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let subtitleLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("analytics_subtitle", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .medium)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let timeFilterButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle("This Week ▾", for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
        b.setTitleColor(DesignTokens.Colors.textMain, for: .normal)
        b.backgroundColor = DesignTokens.Colors.neutralLight
        b.layer.cornerRadius = DesignTokens.Radius.lg
        b.contentEdgeInsets = UIEdgeInsets(top: 8, left: 12, bottom: 8, right: 12)
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    // Chart card
    private let chartCard: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.layer.cornerRadius = DesignTokens.Radius.xl
        v.layer.borderWidth = 1
        v.layer.borderColor = DesignTokens.Colors.neutralLight.cgColor
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let chartTitle: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("weekly_performance", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let chartContainer: UIView = {
        let v = UIView()
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    // Stat cards
    private let statsStack: UIStackView = {
        let s = UIStackView()
        s.axis = .horizontal
        s.distribution = .fillEqually
        s.spacing = DesignTokens.Spacing.base
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        drawChart()
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        // Header section
        let headerSection = UIView()
        headerSection.translatesAutoresizingMaskIntoConstraints = false
        headerSection.backgroundColor = DesignTokens.Colors.surfaceLight
        headerSection.addSubviews(titleLabel, subtitleLabel, timeFilterButton)
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: headerSection.topAnchor, constant: 32),
            titleLabel.leadingAnchor.constraint(equalTo: headerSection.leadingAnchor, constant: DesignTokens.Spacing.xl),
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 4),
            subtitleLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            subtitleLabel.bottomAnchor.constraint(equalTo: headerSection.bottomAnchor, constant: -16),
            timeFilterButton.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
            timeFilterButton.trailingAnchor.constraint(equalTo: headerSection.trailingAnchor, constant: -DesignTokens.Spacing.xl),
        ])

        // Chart
        chartCard.addSubviews(chartTitle, chartContainer)
        NSLayoutConstraint.activate([
            chartTitle.topAnchor.constraint(equalTo: chartCard.topAnchor, constant: DesignTokens.Spacing.xl),
            chartTitle.leadingAnchor.constraint(equalTo: chartCard.leadingAnchor, constant: DesignTokens.Spacing.xl),
            chartContainer.topAnchor.constraint(equalTo: chartTitle.bottomAnchor, constant: DesignTokens.Spacing.xl),
            chartContainer.leadingAnchor.constraint(equalTo: chartCard.leadingAnchor, constant: DesignTokens.Spacing.xl),
            chartContainer.trailingAnchor.constraint(equalTo: chartCard.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            chartContainer.heightAnchor.constraint(equalToConstant: 180),
            chartContainer.bottomAnchor.constraint(equalTo: chartCard.bottomAnchor, constant: -DesignTokens.Spacing.xl),
        ])

        // Stats
        let stats: [(String, String, UIColor)] = [
            ("85%", NSLocalizedString("compliance", comment: ""), DesignTokens.Colors.primary),
            ("↓ 2.1", NSLocalizedString("pain_trend", comment: ""), DesignTokens.Colors.success),
            ("5 Days", NSLocalizedString("streak", comment: ""), DesignTokens.Colors.warning),
        ]

        for (value, label, color) in stats {
            let card = createStatCard(value: value, label: label, color: color)
            statsStack.addArrangedSubview(card)
        }

        // Assemble
        let chartWrapper = UIView()
        chartWrapper.translatesAutoresizingMaskIntoConstraints = false
        chartWrapper.addSubview(chartCard)
        NSLayoutConstraint.activate([
            chartCard.topAnchor.constraint(equalTo: chartWrapper.topAnchor),
            chartCard.leadingAnchor.constraint(equalTo: chartWrapper.leadingAnchor, constant: DesignTokens.Spacing.xl),
            chartCard.trailingAnchor.constraint(equalTo: chartWrapper.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            chartCard.bottomAnchor.constraint(equalTo: chartWrapper.bottomAnchor),
        ])

        let statsWrapper = UIView()
        statsWrapper.translatesAutoresizingMaskIntoConstraints = false
        statsWrapper.addSubview(statsStack)
        NSLayoutConstraint.activate([
            statsStack.topAnchor.constraint(equalTo: statsWrapper.topAnchor),
            statsStack.leadingAnchor.constraint(equalTo: statsWrapper.leadingAnchor, constant: DesignTokens.Spacing.xl),
            statsStack.trailingAnchor.constraint(equalTo: statsWrapper.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            statsStack.bottomAnchor.constraint(equalTo: statsWrapper.bottomAnchor),
        ])

        contentStack.addArrangedSubview(headerSection)
        contentStack.addArrangedSubview(chartWrapper)
        contentStack.addArrangedSubview(statsWrapper)

        scrollView.addSubview(contentStack)
        view.addSubview(scrollView)

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            contentStack.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentStack.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentStack.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentStack.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor, constant: -DesignTokens.Spacing.xl),
            contentStack.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
        ])
    }

    // MARK: - Chart

    private func drawChart() {
        chartContainer.layer.sublayers?.removeAll()

        let width = chartContainer.bounds.width
        let height = chartContainer.bounds.height - 20
        guard width > 0 else { return }

        let barSpacing = width / CGFloat(weeklyData.count)
        let barWidth: CGFloat = 12

        for (i, d) in weeklyData.enumerated() {
            let x = barSpacing * CGFloat(i) + (barSpacing - barWidth) / 2
            let barHeight = height * d.activity

            // Activity bar
            let barLayer = CALayer()
            barLayer.frame = CGRect(x: x, y: height - barHeight, width: barWidth, height: barHeight)
            barLayer.backgroundColor = DesignTokens.Colors.chartGrey.cgColor
            barLayer.cornerRadius = barWidth / 2
            barLayer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
            chartContainer.layer.addSublayer(barLayer)

            // Pain dot
            let dotY = height - height * d.painDot
            let dotLayer = CALayer()
            dotLayer.frame = CGRect(x: x + barWidth / 2 - 5, y: dotY - 5, width: 10, height: 10)
            dotLayer.backgroundColor = DesignTokens.Colors.primary.cgColor
            dotLayer.cornerRadius = 5
            dotLayer.borderWidth = 2
            dotLayer.borderColor = DesignTokens.Colors.surfaceLight.cgColor
            chartContainer.layer.addSublayer(dotLayer)

            // Day label
            let dayLabel = CATextLayer()
            dayLabel.string = d.day
            dayLabel.font = UIFont.systemFont(ofSize: 12, weight: .medium) as CFTypeRef
            dayLabel.fontSize = 12
            dayLabel.foregroundColor = DesignTokens.Colors.textSub.cgColor
            dayLabel.alignmentMode = .center
            dayLabel.contentsScale = UIScreen.main.scale
            dayLabel.frame = CGRect(x: x - 6, y: height + 4, width: barWidth + 12, height: 16)
            chartContainer.layer.addSublayer(dayLabel)
        }
    }

    private func createStatCard(value: String, label: String, color: UIColor) -> UIView {
        let card = UIView()
        card.backgroundColor = DesignTokens.Colors.surfaceLight
        card.layer.cornerRadius = DesignTokens.Radius.xl
        card.layer.borderWidth = 1
        card.layer.borderColor = DesignTokens.Colors.neutralLight.cgColor
        card.translatesAutoresizingMaskIntoConstraints = false

        let valueLbl = UILabel()
        valueLbl.text = value
        valueLbl.font = DesignTokens.Typography.inter(DesignTokens.Typography.title, weight: .bold)
        valueLbl.textColor = color
        valueLbl.textAlignment = .center
        valueLbl.translatesAutoresizingMaskIntoConstraints = false

        let labelLbl = UILabel()
        labelLbl.text = label
        labelLbl.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        labelLbl.textColor = DesignTokens.Colors.textSub
        labelLbl.textAlignment = .center
        labelLbl.translatesAutoresizingMaskIntoConstraints = false

        card.addSubviews(valueLbl, labelLbl)
        NSLayoutConstraint.activate([
            card.heightAnchor.constraint(equalToConstant: 90),
            valueLbl.centerXAnchor.constraint(equalTo: card.centerXAnchor),
            valueLbl.topAnchor.constraint(equalTo: card.topAnchor, constant: 20),
            labelLbl.topAnchor.constraint(equalTo: valueLbl.bottomAnchor, constant: 4),
            labelLbl.centerXAnchor.constraint(equalTo: card.centerXAnchor),
        ])

        return card
    }
}
