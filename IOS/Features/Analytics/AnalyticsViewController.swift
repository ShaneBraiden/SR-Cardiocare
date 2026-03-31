// AnalyticsViewController.swift
// SR-Cardiocare — Patient Progress Analytics
// Stitch Screen: 49ed11ea (08_analytics)
// Shows: donut chart with click-to-reveal info, stat cards (compliance, pain trend, streak), time filter

import UIKit

final class AnalyticsViewController: UIViewController {

    // MARK: - Data

    private struct ChartSegment {
        let label: String
        let value: CGFloat
        let color: UIColor
    }

    private let chartSegments: [ChartSegment] = [
        ChartSegment(label: "Completed", value: 65, color: DesignTokens.Colors.primary),
        ChartSegment(label: "In Progress", value: 20, color: DesignTokens.Colors.chartSecondaryTeal),
        ChartSegment(label: "Missed", value: 15, color: DesignTokens.Colors.error),
    ]

    private var selectedSegmentIndex: Int? = nil

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

    // Center label in donut
    private let centerValueLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.hero, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let centerDescLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        l.textColor = DesignTokens.Colors.textSub
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    // Tooltip card
    private let tooltipCard: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.layer.cornerRadius = DesignTokens.Radius.lg
        v.layer.shadowColor = UIColor.black.cgColor
        v.layer.shadowOpacity = 0.15
        v.layer.shadowRadius = 8
        v.layer.shadowOffset = CGSize(width: 0, height: 4)
        v.isHidden = true
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let tooltipLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.numberOfLines = 2
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    // Legend
    private let legendStack: UIStackView = {
        let s = UIStackView()
        s.axis = .horizontal
        s.distribution = .fillEqually
        s.spacing = DesignTokens.Spacing.sm
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
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
        setupGestures()
        updateCenterLabel(total: true)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        drawDonutChart()
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

        // Center labels
        chartContainer.addSubviews(centerValueLabel, centerDescLabel, tooltipCard)
        tooltipCard.addSubview(tooltipLabel)

        NSLayoutConstraint.activate([
            centerValueLabel.centerXAnchor.constraint(equalTo: chartContainer.centerXAnchor),
            centerValueLabel.centerYAnchor.constraint(equalTo: chartContainer.centerYAnchor, constant: -10),
            centerDescLabel.topAnchor.constraint(equalTo: centerValueLabel.bottomAnchor, constant: 4),
            centerDescLabel.centerXAnchor.constraint(equalTo: chartContainer.centerXAnchor),

            tooltipCard.centerXAnchor.constraint(equalTo: chartContainer.centerXAnchor),
            tooltipCard.centerYAnchor.constraint(equalTo: chartContainer.centerYAnchor),
            tooltipCard.widthAnchor.constraint(equalToConstant: 140),
            tooltipCard.heightAnchor.constraint(equalToConstant: 60),

            tooltipLabel.centerXAnchor.constraint(equalTo: tooltipCard.centerXAnchor),
            tooltipLabel.centerYAnchor.constraint(equalTo: tooltipCard.centerYAnchor),
            tooltipLabel.leadingAnchor.constraint(equalTo: tooltipCard.leadingAnchor, constant: 8),
            tooltipLabel.trailingAnchor.constraint(equalTo: tooltipCard.trailingAnchor, constant: -8),
        ])

        // Legend
        for segment in chartSegments {
            let item = createLegendItem(color: segment.color, label: segment.label)
            legendStack.addArrangedSubview(item)
        }

        // Chart card
        chartCard.addSubviews(chartTitle, chartContainer, legendStack)
        NSLayoutConstraint.activate([
            chartTitle.topAnchor.constraint(equalTo: chartCard.topAnchor, constant: DesignTokens.Spacing.xl),
            chartTitle.leadingAnchor.constraint(equalTo: chartCard.leadingAnchor, constant: DesignTokens.Spacing.xl),
            chartContainer.topAnchor.constraint(equalTo: chartTitle.bottomAnchor, constant: DesignTokens.Spacing.lg),
            chartContainer.centerXAnchor.constraint(equalTo: chartCard.centerXAnchor),
            chartContainer.widthAnchor.constraint(equalToConstant: 200),
            chartContainer.heightAnchor.constraint(equalToConstant: 200),
            legendStack.topAnchor.constraint(equalTo: chartContainer.bottomAnchor, constant: DesignTokens.Spacing.lg),
            legendStack.leadingAnchor.constraint(equalTo: chartCard.leadingAnchor, constant: DesignTokens.Spacing.xl),
            legendStack.trailingAnchor.constraint(equalTo: chartCard.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            legendStack.bottomAnchor.constraint(equalTo: chartCard.bottomAnchor, constant: -DesignTokens.Spacing.xl),
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

    private func setupGestures() {
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(chartTapped(_:)))
        chartContainer.addGestureRecognizer(tapGesture)
        chartContainer.isUserInteractionEnabled = true
    }

    // MARK: - Chart

    private func drawDonutChart() {
        chartContainer.layer.sublayers?.forEach { layer in
            if layer is CAShapeLayer {
                layer.removeFromSuperlayer()
            }
        }

        let size = chartContainer.bounds.size
        guard size.width > 0 else { return }

        let center = CGPoint(x: size.width / 2, y: size.height / 2)
        let radius: CGFloat = min(size.width, size.height) / 2 - 10
        let lineWidth: CGFloat = 28

        let total = chartSegments.reduce(0) { $0 + $1.value }
        var startAngle: CGFloat = -.pi / 2

        for (index, segment) in chartSegments.enumerated() {
            let endAngle = startAngle + (segment.value / total) * 2 * .pi

            let path = UIBezierPath(arcCenter: center, radius: radius - lineWidth / 2, startAngle: startAngle, endAngle: endAngle, clockwise: true)

            let shapeLayer = CAShapeLayer()
            shapeLayer.path = path.cgPath
            shapeLayer.strokeColor = segment.color.cgColor
            shapeLayer.fillColor = UIColor.clear.cgColor
            shapeLayer.lineWidth = lineWidth
            shapeLayer.lineCap = .butt
            shapeLayer.name = "segment_\(index)"

            // Highlight selected segment
            if selectedSegmentIndex == index {
                shapeLayer.lineWidth = lineWidth + 6
            }

            chartContainer.layer.insertSublayer(shapeLayer, at: 0)
            startAngle = endAngle
        }
    }

    @objc private func chartTapped(_ gesture: UITapGestureRecognizer) {
        let location = gesture.location(in: chartContainer)
        let size = chartContainer.bounds.size
        let center = CGPoint(x: size.width / 2, y: size.height / 2)
        let radius: CGFloat = min(size.width, size.height) / 2 - 10
        let lineWidth: CGFloat = 28

        // Calculate distance from center
        let dx = location.x - center.x
        let dy = location.y - center.y
        let distance = sqrt(dx * dx + dy * dy)

        // Check if tap is within the donut ring
        let innerRadius = radius - lineWidth
        let outerRadius = radius

        if distance >= innerRadius && distance <= outerRadius {
            // Calculate angle
            var angle = atan2(dy, dx)
            if angle < -.pi / 2 {
                angle += 2 * .pi
            }
            angle += .pi / 2
            if angle > 2 * .pi {
                angle -= 2 * .pi
            }

            // Find which segment was tapped
            let total = chartSegments.reduce(0) { $0 + $1.value }
            var currentAngle: CGFloat = 0

            for (index, segment) in chartSegments.enumerated() {
                let segmentAngle = (segment.value / total) * 2 * .pi
                if angle >= currentAngle && angle < currentAngle + segmentAngle {
                    handleSegmentTap(index: index)
                    return
                }
                currentAngle += segmentAngle
            }
        } else if distance < innerRadius {
            // Tapped center - deselect
            selectedSegmentIndex = nil
            tooltipCard.isHidden = true
            centerValueLabel.isHidden = false
            centerDescLabel.isHidden = false
            updateCenterLabel(total: true)
            drawDonutChart()
        }
    }

    private func handleSegmentTap(index: Int) {
        if selectedSegmentIndex == index {
            // Deselect
            selectedSegmentIndex = nil
            tooltipCard.isHidden = true
            centerValueLabel.isHidden = false
            centerDescLabel.isHidden = false
            updateCenterLabel(total: true)
        } else {
            // Select new segment
            selectedSegmentIndex = index
            let segment = chartSegments[index]
            tooltipLabel.text = "\(segment.label)\n\(Int(segment.value))%"
            tooltipLabel.textColor = segment.color

            centerValueLabel.isHidden = true
            centerDescLabel.isHidden = true
            tooltipCard.isHidden = false
        }
        drawDonutChart()
    }

    private func updateCenterLabel(total: Bool) {
        if total {
            let totalValue = Int(chartSegments.reduce(0) { $0 + $1.value })
            centerValueLabel.text = "\(totalValue)%"
            centerDescLabel.text = "Total"
        }
    }

    private func createLegendItem(color: UIColor, label: String) -> UIView {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false

        let dot = UIView()
        dot.backgroundColor = color
        dot.layer.cornerRadius = 5
        dot.translatesAutoresizingMaskIntoConstraints = false

        let lbl = UILabel()
        lbl.text = label
        lbl.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        lbl.textColor = DesignTokens.Colors.textSub
        lbl.translatesAutoresizingMaskIntoConstraints = false

        container.addSubviews(dot, lbl)
        NSLayoutConstraint.activate([
            dot.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            dot.centerYAnchor.constraint(equalTo: container.centerYAnchor),
            dot.widthAnchor.constraint(equalToConstant: 10),
            dot.heightAnchor.constraint(equalToConstant: 10),
            lbl.leadingAnchor.constraint(equalTo: dot.trailingAnchor, constant: 6),
            lbl.centerYAnchor.constraint(equalTo: container.centerYAnchor),
            lbl.trailingAnchor.constraint(lessThanOrEqualTo: container.trailingAnchor),
            container.heightAnchor.constraint(equalToConstant: 24),
        ])

        return container
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
