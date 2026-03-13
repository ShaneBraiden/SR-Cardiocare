// OnboardingGoalsViewController.swift
// SR-Cardiocare — Onboarding Step 3/3: Goal Selection
// Stitch Screen: 50742b82 (10_onboarding_goals)
// Single-select radio list: Reduce Pain, Increase Mobility, Post-Surgery Recovery, Sports Performance

import UIKit

final class OnboardingGoalsViewController: UIViewController {

    // MARK: - Properties

    var selectedInjuries: [String] = []

    private let goals: [(title: String, icon: String)] = [
        ("Reduce Pain", "cross.case.fill"),
        ("Increase Mobility", "figure.walk"),
        ("Post-Surgery Recovery", "stethoscope"),
        ("Sports Performance", "figure.strengthtraining.traditional"),
    ]

    private var selectedGoalIndex: Int? = nil

    // MARK: - UI

    private let progressStack: UIStackView = {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.spacing = DesignTokens.Spacing.sm
        stack.alignment = .center
        stack.translatesAutoresizingMaskIntoConstraints = false
        let dots: [(CGFloat, Bool)] = [(10, false), (10, false), (32, true)]
        for (w, active) in dots {
            let dot = UIView()
            dot.backgroundColor = active ? DesignTokens.Colors.primary : DesignTokens.Colors.neutralGrey
            dot.layer.cornerRadius = 5
            dot.translatesAutoresizingMaskIntoConstraints = false
            dot.widthAnchor.constraint(equalToConstant: w).isActive = true
            dot.heightAnchor.constraint(equalToConstant: 10).isActive = true
            stack.addArrangedSubview(dot)
        }
        return stack
    }()

    private let titleLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("onboarding_goals_title", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.hero, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.numberOfLines = 0
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let descLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("onboarding_goals_desc", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        l.textColor = DesignTokens.Colors.textSub
        l.numberOfLines = 0
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let goalsStack: UIStackView = {
        let s = UIStackView()
        s.axis = .vertical
        s.spacing = DesignTokens.Spacing.base
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    private let getStartedButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("onboarding_get_started", comment: ""), for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        b.setTitleColor(.white, for: .normal)
        b.backgroundColor = DesignTokens.Colors.primary
        b.layer.cornerRadius = 28
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        buildGoalCards()
        getStartedButton.addTarget(self, action: #selector(getStartedTapped), for: .touchUpInside)
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        let scrollView = UIScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubviews(scrollView, getStartedButton)

        let contentView = UIView()
        contentView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(contentView)

        contentView.addSubviews(progressStack, titleLabel, descLabel, goalsStack)

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: getStartedButton.topAnchor, constant: -DesignTokens.Spacing.base),

            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),

            progressStack.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            progressStack.topAnchor.constraint(equalTo: contentView.topAnchor, constant: DesignTokens.Spacing.xl),

            titleLabel.topAnchor.constraint(equalTo: progressStack.bottomAnchor, constant: DesignTokens.Spacing.xxl),
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: DesignTokens.Spacing.xl),
            titleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -DesignTokens.Spacing.xl),

            descLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: DesignTokens.Spacing.sm),
            descLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            descLabel.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor),

            goalsStack.topAnchor.constraint(equalTo: descLabel.bottomAnchor, constant: DesignTokens.Spacing.xxl),
            goalsStack.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: DesignTokens.Spacing.xl),
            goalsStack.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            goalsStack.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -DesignTokens.Spacing.xl),

            getStartedButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            getStartedButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            getStartedButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -DesignTokens.Spacing.xxl),
            getStartedButton.heightAnchor.constraint(equalToConstant: 56),
        ])
    }

    private func buildGoalCards() {
        for (index, goal) in goals.enumerated() {
            let card = GoalCardView(title: goal.title, iconName: goal.icon, tag: index)
            card.addTarget(self, action: #selector(goalSelected(_:)), for: .touchUpInside)
            goalsStack.addArrangedSubview(card)
        }
    }

    // MARK: - Actions

    @objc private func goalSelected(_ sender: GoalCardView) {
        selectedGoalIndex = sender.tag
        for case let card as GoalCardView in goalsStack.arrangedSubviews {
            card.setSelected(card.tag == sender.tag)
        }
    }

    @objc private func getStartedTapped() {
        guard let goalIdx = selectedGoalIndex else { return }
        let goal = goals[goalIdx].title

        // Save onboarding data
        UserDefaults.standard.set(selectedInjuries, forKey: "onboarding_injuries")
        UserDefaults.standard.set(goal, forKey: "onboarding_goal")
        UserDefaults.standard.set(true, forKey: "onboarding_completed")

        // Navigate to patient home
        let homeVC = PatientHomeViewController()
        let nav = UINavigationController(rootViewController: homeVC)
        nav.modalPresentationStyle = .fullScreen
        present(nav, animated: true)
    }
}

// MARK: - GoalCardView

final class GoalCardView: UIControl {

    private let iconBG: UIView = {
        let v = UIView()
        v.layer.cornerRadius = 20
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let iconView: UIImageView = {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFit
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    private let titleLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let radioView: UIView = {
        let v = UIView()
        v.layer.cornerRadius = 12
        v.layer.borderWidth = 2
        v.layer.borderColor = DesignTokens.Colors.neutralGrey.cgColor
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let radioFill: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.primary
        v.layer.cornerRadius = 6
        v.isHidden = true
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let borderOverlay: UIView = {
        let v = UIView()
        v.layer.cornerRadius = DesignTokens.Radius.lg
        v.layer.borderWidth = 2
        v.layer.borderColor = UIColor.clear.cgColor
        v.isUserInteractionEnabled = false
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    init(title: String, iconName: String, tag: Int) {
        super.init(frame: .zero)
        self.tag = tag
        titleLabel.text = title
        iconView.image = UIImage(systemName: iconName)
        setupView()
        setSelected(false)
    }

    required init?(coder: NSCoder) { fatalError() }

    private func setupView() {
        backgroundColor = DesignTokens.Colors.surfaceLight
        layer.cornerRadius = DesignTokens.Radius.lg
        translatesAutoresizingMaskIntoConstraints = false
        heightAnchor.constraint(equalToConstant: 72).isActive = true

        addSubviews(iconBG, titleLabel, radioView, borderOverlay)
        iconBG.addSubview(iconView)
        radioView.addSubview(radioFill)

        NSLayoutConstraint.activate([
            iconBG.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 20),
            iconBG.centerYAnchor.constraint(equalTo: centerYAnchor),
            iconBG.widthAnchor.constraint(equalToConstant: 40),
            iconBG.heightAnchor.constraint(equalToConstant: 40),

            iconView.centerXAnchor.constraint(equalTo: iconBG.centerXAnchor),
            iconView.centerYAnchor.constraint(equalTo: iconBG.centerYAnchor),
            iconView.widthAnchor.constraint(equalToConstant: 22),
            iconView.heightAnchor.constraint(equalToConstant: 22),

            titleLabel.leadingAnchor.constraint(equalTo: iconBG.trailingAnchor, constant: DesignTokens.Spacing.base),
            titleLabel.centerYAnchor.constraint(equalTo: centerYAnchor),

            radioView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -20),
            radioView.centerYAnchor.constraint(equalTo: centerYAnchor),
            radioView.widthAnchor.constraint(equalToConstant: 24),
            radioView.heightAnchor.constraint(equalToConstant: 24),

            radioFill.centerXAnchor.constraint(equalTo: radioView.centerXAnchor),
            radioFill.centerYAnchor.constraint(equalTo: radioView.centerYAnchor),
            radioFill.widthAnchor.constraint(equalToConstant: 12),
            radioFill.heightAnchor.constraint(equalToConstant: 12),

            borderOverlay.topAnchor.constraint(equalTo: topAnchor),
            borderOverlay.leadingAnchor.constraint(equalTo: leadingAnchor),
            borderOverlay.trailingAnchor.constraint(equalTo: trailingAnchor),
            borderOverlay.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])
    }

    func setSelected(_ selected: Bool) {
        if selected {
            borderOverlay.layer.borderColor = DesignTokens.Colors.primary.cgColor
            iconBG.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.1)
            iconView.tintColor = DesignTokens.Colors.primary
            radioView.layer.borderColor = DesignTokens.Colors.primary.cgColor
            radioFill.isHidden = false
        } else {
            borderOverlay.layer.borderColor = UIColor.clear.cgColor
            iconBG.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.1)
            iconView.tintColor = DesignTokens.Colors.primary
            radioView.layer.borderColor = DesignTokens.Colors.neutralGrey.cgColor
            radioFill.isHidden = true
        }
    }
}
