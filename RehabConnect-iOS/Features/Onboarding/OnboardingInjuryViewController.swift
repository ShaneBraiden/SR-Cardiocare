// OnboardingInjuryViewController.swift
// SR-Cardiocare — Onboarding Step 2/3: Injury Selection
// Stitch Screen: b4fc7fba (20_onboarding_injury)
// Multi-select grid of body parts (Knee, Shoulder, Lower Back, Neck, Ankle, Hip)

import UIKit

final class OnboardingInjuryViewController: UIViewController {

    // MARK: - Types

    struct BodyPart {
        let name: String
        let subtitle: String
        let iconName: String       // SF Symbol
        var isSelected: Bool = false
    }

    // MARK: - Properties

    private var bodyParts: [BodyPart] = [
        BodyPart(name: "Knee", subtitle: "Joint & Ligaments", iconName: "figure.walk"),
        BodyPart(name: "Shoulder", subtitle: "Rotator Cuff", iconName: "figure.arms.open"),
        BodyPart(name: "Lower Back", subtitle: "Lumbar Spine", iconName: "figure.stand"),
        BodyPart(name: "Neck", subtitle: "Cervical Spine", iconName: "person.crop.circle"),
        BodyPart(name: "Ankle", subtitle: "Foot & Achilles", iconName: "shoeprint.fill"),
        BodyPart(name: "Hip", subtitle: "Joint & Pelvis", iconName: "figure.cooldown"),
    ]

    // MARK: - UI

    private let stepLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("onboarding_step_2", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .medium)
        l.textColor = DesignTokens.Colors.textSub
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let progressStack: UIStackView = {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.spacing = DesignTokens.Spacing.sm
        stack.alignment = .center
        stack.translatesAutoresizingMaskIntoConstraints = false
        let dots: [(CGFloat, Bool)] = [(10, false), (32, true), (10, false)]
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
        l.text = NSLocalizedString("onboarding_injury_title", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.hero, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.numberOfLines = 0
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let descLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("onboarding_injury_desc", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        l.textColor = DesignTokens.Colors.textSub
        l.numberOfLines = 0
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private lazy var collectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.minimumInteritemSpacing = DesignTokens.Spacing.base
        layout.minimumLineSpacing = DesignTokens.Spacing.base
        let cv = UICollectionView(frame: .zero, collectionViewLayout: layout)
        cv.backgroundColor = .clear
        cv.dataSource = self
        cv.delegate = self
        cv.allowsMultipleSelection = true
        cv.register(BodyPartCell.self, forCellWithReuseIdentifier: BodyPartCell.reuseID)
        cv.translatesAutoresizingMaskIntoConstraints = false
        return cv
    }()

    private let nextButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("onboarding_next_button", comment: ""), for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        b.setTitleColor(.white, for: .normal)
        b.backgroundColor = DesignTokens.Colors.primary
        b.layer.cornerRadius = DesignTokens.Radius.button
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        nextButton.addTarget(self, action: #selector(nextTapped), for: .touchUpInside)
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        view.addSubviews(progressStack, stepLabel, titleLabel, descLabel, collectionView, nextButton)

        NSLayoutConstraint.activate([
            progressStack.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            progressStack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: DesignTokens.Spacing.xl),

            stepLabel.topAnchor.constraint(equalTo: progressStack.bottomAnchor, constant: DesignTokens.Spacing.sm),
            stepLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),

            titleLabel.topAnchor.constraint(equalTo: stepLabel.bottomAnchor, constant: DesignTokens.Spacing.xl),
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            titleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),

            descLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: DesignTokens.Spacing.sm),
            descLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            descLabel.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor),

            collectionView.topAnchor.constraint(equalTo: descLabel.bottomAnchor, constant: DesignTokens.Spacing.xxl),
            collectionView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            collectionView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            collectionView.bottomAnchor.constraint(equalTo: nextButton.topAnchor, constant: -DesignTokens.Spacing.xl),

            nextButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            nextButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            nextButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -DesignTokens.Spacing.xl),
            nextButton.heightAnchor.constraint(equalToConstant: 56),
        ])
    }

    // MARK: - Actions

    @objc private func nextTapped() {
        let selected = bodyParts.enumerated().filter { bodyParts[$0.offset].isSelected }.map { $0.element.name }
        guard !selected.isEmpty else { return }
        let goalsVC = OnboardingGoalsViewController()
        goalsVC.selectedInjuries = selected
        navigationController?.pushViewController(goalsVC, animated: true)
    }
}

// MARK: - UICollectionView

extension OnboardingInjuryViewController: UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int { bodyParts.count }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: BodyPartCell.reuseID, for: indexPath) as! BodyPartCell
        cell.configure(with: bodyParts[indexPath.item])
        return cell
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let spacing = DesignTokens.Spacing.base
        let width = (collectionView.bounds.width - spacing) / 2
        return CGSize(width: width, height: 140)
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        bodyParts[indexPath.item].isSelected.toggle()
        collectionView.reloadItems(at: [indexPath])
    }

    func collectionView(_ collectionView: UICollectionView, didDeselectItemAt indexPath: IndexPath) {
        bodyParts[indexPath.item].isSelected = false
        collectionView.reloadItems(at: [indexPath])
    }
}

// MARK: - BodyPartCell

final class BodyPartCell: UICollectionViewCell {

    static let reuseID = "BodyPartCell"

    private let iconContainer: UIView = {
        let v = UIView()
        v.layer.cornerRadius = DesignTokens.Radius.lg
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let iconImageView: UIImageView = {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFit
        iv.tintColor = DesignTokens.Colors.textSub
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    private let nameLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let subtitleLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let checkIcon: UIImageView = {
        let iv = UIImageView()
        iv.image = UIImage(systemName: "checkmark.circle.fill")
        iv.tintColor = DesignTokens.Colors.primary
        iv.translatesAutoresizingMaskIntoConstraints = false
        iv.isHidden = true
        return iv
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupCell()
    }

    required init?(coder: NSCoder) { fatalError() }

    private func setupCell() {
        contentView.layer.cornerRadius = DesignTokens.Radius.xl
        contentView.layer.borderWidth = 2
        contentView.layer.borderColor = UIColor.clear.cgColor
        contentView.backgroundColor = DesignTokens.Colors.surfaceLight

        contentView.addSubviews(checkIcon, iconContainer, nameLabel, subtitleLabel)
        iconContainer.addSubview(iconImageView)

        NSLayoutConstraint.activate([
            checkIcon.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            checkIcon.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -12),
            checkIcon.widthAnchor.constraint(equalToConstant: 20),
            checkIcon.heightAnchor.constraint(equalToConstant: 20),

            iconContainer.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 16),
            iconContainer.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            iconContainer.widthAnchor.constraint(equalToConstant: 48),
            iconContainer.heightAnchor.constraint(equalToConstant: 48),

            iconImageView.centerXAnchor.constraint(equalTo: iconContainer.centerXAnchor),
            iconImageView.centerYAnchor.constraint(equalTo: iconContainer.centerYAnchor),
            iconImageView.widthAnchor.constraint(equalToConstant: 28),
            iconImageView.heightAnchor.constraint(equalToConstant: 28),

            nameLabel.topAnchor.constraint(equalTo: iconContainer.bottomAnchor, constant: 12),
            nameLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            nameLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),

            subtitleLabel.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 4),
            subtitleLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),
        ])
    }

    func configure(with part: OnboardingInjuryViewController.BodyPart) {
        nameLabel.text = part.name
        subtitleLabel.text = part.subtitle
        iconImageView.image = UIImage(systemName: part.iconName)

        if part.isSelected {
            contentView.layer.borderColor = DesignTokens.Colors.primary.cgColor
            iconContainer.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.1)
            iconImageView.tintColor = DesignTokens.Colors.primary
            checkIcon.isHidden = false
        } else {
            contentView.layer.borderColor = UIColor.clear.cgColor
            iconContainer.backgroundColor = DesignTokens.Colors.backgroundLight
            iconImageView.tintColor = DesignTokens.Colors.textSub
            checkIcon.isHidden = true
        }
    }
}
