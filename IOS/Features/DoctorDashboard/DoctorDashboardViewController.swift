// DoctorDashboardViewController.swift
// SR-Cardiocare — Doctor Dashboard with patient overview
// Stitch Screens: 7084435b (13_dashboard_v1), c51d9ae9 (26_dashboard_v2)
// Shows: greeting, patient list with status badges, FAB for add patient

import UIKit

final class DoctorDashboardViewController: UIViewController {

    // MARK: - Models

    struct PatientOverviewItem {
        let id: String
        let name: String
        let condition: String
        let lastActivity: String
        let isOnline: Bool
        let completedWorkouts: Int
        let totalWorkouts: Int

        // On Track = 100% completion
        var status: PatientStatus {
            if totalWorkouts == 0 {
                return .inactive
            }
            let completionRate = Double(completedWorkouts) / Double(totalWorkouts)
            if completionRate >= 1.0 {
                return .onTrack
            } else if completionRate >= 0.5 {
                return .needsAttention
            } else {
                return .inactive
            }
        }
    }

    enum PatientStatus: String {
        case onTrack = "On Track"
        case needsAttention = "Needs Attention"
        case inactive = "Inactive"
    }

    // MARK: - Data

    private var patients: [PatientOverviewItem] = [
        PatientOverviewItem(id: "1", name: "Sarah Wilson", condition: "ACL Recovery", lastActivity: "2h ago", isOnline: true, completedWorkouts: 10, totalWorkouts: 10),
        PatientOverviewItem(id: "2", name: "James Chen", condition: "Rotator Cuff", lastActivity: "5h ago", isOnline: false, completedWorkouts: 6, totalWorkouts: 10),
        PatientOverviewItem(id: "3", name: "Maria Garcia", condition: "Lower Back Rehab", lastActivity: "1h ago", isOnline: true, completedWorkouts: 8, totalWorkouts: 8),
        PatientOverviewItem(id: "4", name: "David Johnson", condition: "Knee Replacement", lastActivity: "2d ago", isOnline: false, completedWorkouts: 2, totalWorkouts: 10),
        PatientOverviewItem(id: "5", name: "Emily Brown", condition: "Ankle Sprain", lastActivity: "30m ago", isOnline: true, completedWorkouts: 5, totalWorkouts: 5),
    ]

    // MARK: - UI

    private let headerView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let welcomeLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("welcome_back", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .medium)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let doctorNameLabel: UILabel = {
        let l = UILabel()
        l.text = "Dr. Smith"
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.title, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let notificationButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "bell.fill"), for: .normal)
        b.tintColor = DesignTokens.Colors.textMain
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let notificationDot: UIView = {
        let v = UIView()
        v.backgroundColor = .systemRed
        v.layer.cornerRadius = 5
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()



    private let sectionHeader: UIView = {
        let v = UIView()
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let sectionTitle: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("patient_overview", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let viewAllButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("view_all", comment: ""), for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .medium)
        b.setTitleColor(DesignTokens.Colors.primary, for: .normal)
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let tableView: UITableView = {
        let tv = UITableView(frame: .zero, style: .plain)
        tv.separatorStyle = .none
        tv.backgroundColor = .clear
        tv.translatesAutoresizingMaskIntoConstraints = false
        return tv
    }()

    private let addPatientFAB: UIButton = {
        let b = UIButton(type: .system)
        let config = UIImage.SymbolConfiguration(pointSize: 24, weight: .medium)
        b.setImage(UIImage(systemName: "plus", withConfiguration: config), for: .normal)
        b.tintColor = .white
        b.backgroundColor = DesignTokens.Colors.primary
        b.layer.cornerRadius = 28
        b.layer.shadowColor = DesignTokens.Colors.primary.cgColor
        b.layer.shadowOpacity = 0.3
        b.layer.shadowRadius = 10
        b.layer.shadowOffset = CGSize(width: 0, height: 4)
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupActions()
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        // Header
        headerView.addSubviews(welcomeLabel, doctorNameLabel, notificationButton, notificationDot)
        sectionHeader.addSubviews(sectionTitle, viewAllButton)

        NSLayoutConstraint.activate([
            welcomeLabel.topAnchor.constraint(equalTo: headerView.topAnchor, constant: 32),
            welcomeLabel.leadingAnchor.constraint(equalTo: headerView.leadingAnchor, constant: 20),

            doctorNameLabel.topAnchor.constraint(equalTo: welcomeLabel.bottomAnchor, constant: 2),
            doctorNameLabel.leadingAnchor.constraint(equalTo: welcomeLabel.leadingAnchor),

            notificationButton.topAnchor.constraint(equalTo: welcomeLabel.topAnchor),
            notificationButton.trailingAnchor.constraint(equalTo: headerView.trailingAnchor, constant: -20),
            notificationButton.widthAnchor.constraint(equalToConstant: 40),
            notificationButton.heightAnchor.constraint(equalToConstant: 40),

            notificationDot.topAnchor.constraint(equalTo: notificationButton.topAnchor, constant: 6),
            notificationDot.trailingAnchor.constraint(equalTo: notificationButton.trailingAnchor, constant: -8),
            notificationDot.widthAnchor.constraint(equalToConstant: 10),
            notificationDot.heightAnchor.constraint(equalToConstant: 10),

            doctorNameLabel.bottomAnchor.constraint(equalTo: headerView.bottomAnchor, constant: -16),

            sectionTitle.leadingAnchor.constraint(equalTo: sectionHeader.leadingAnchor, constant: 16),
            sectionTitle.centerYAnchor.constraint(equalTo: sectionHeader.centerYAnchor),
            viewAllButton.trailingAnchor.constraint(equalTo: sectionHeader.trailingAnchor, constant: -16),
            viewAllButton.centerYAnchor.constraint(equalTo: sectionHeader.centerYAnchor),
            sectionHeader.heightAnchor.constraint(equalToConstant: 44),
        ])

        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(PatientOverviewCell.self, forCellReuseIdentifier: PatientOverviewCell.reuseID)

        view.addSubviews(headerView, sectionHeader, tableView, addPatientFAB)

        NSLayoutConstraint.activate([
            headerView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            headerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),

            sectionHeader.topAnchor.constraint(equalTo: headerView.bottomAnchor, constant: 8),
            sectionHeader.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            sectionHeader.trailingAnchor.constraint(equalTo: view.trailingAnchor),

            tableView.topAnchor.constraint(equalTo: sectionHeader.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            addPatientFAB.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            addPatientFAB.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            addPatientFAB.widthAnchor.constraint(equalToConstant: 56),
            addPatientFAB.heightAnchor.constraint(equalToConstant: 56),
        ])
    }

    private func setupActions() {
        addPatientFAB.addTarget(self, action: #selector(addPatientTapped), for: .touchUpInside)
    }

    // MARK: - Actions

    @objc private func addPatientTapped() {
        let addVC = AddPatientViewController()
        navigationController?.pushViewController(addVC, animated: true)
    }

}

// MARK: - Table View

extension DoctorDashboardViewController: UITableViewDataSource, UITableViewDelegate {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { patients.count }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat { 88 }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: PatientOverviewCell.reuseID, for: indexPath) as! PatientOverviewCell
        cell.configure(with: patients[indexPath.row])
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let profileVC = PatientProfileViewController()
        navigationController?.pushViewController(profileVC, animated: true)
    }
}

// MARK: - PatientOverviewCell

final class PatientOverviewCell: UITableViewCell {

    static let reuseID = "PatientOverviewCell"

    private let card: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.layer.cornerRadius = DesignTokens.Radius.xxl
        v.layer.borderWidth = 1
        v.layer.borderColor = DesignTokens.Colors.neutralLight.cgColor
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let avatarView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.primaryLight.withAlphaComponent(0.3)
        v.layer.cornerRadius = 28
        v.layer.borderWidth = 2
        v.layer.borderColor = UIColor.white.cgColor
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let avatarLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.primary
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let nameLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.body, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let conditionLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let statusBadge: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .medium)
        l.textAlignment = .center
        l.layer.cornerRadius = 12
        l.clipsToBounds = true
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let activityLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setup()
    }

    required init?(coder: NSCoder) { fatalError() }

    private func setup() {
        backgroundColor = .clear
        selectionStyle = .none
        contentView.addSubview(card)
        card.addSubviews(avatarView, nameLabel, conditionLabel, statusBadge, activityLabel)
        avatarView.addSubview(avatarLabel)

        NSLayoutConstraint.activate([
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 4),
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -4),

            avatarView.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            avatarView.centerYAnchor.constraint(equalTo: card.centerYAnchor),
            avatarView.widthAnchor.constraint(equalToConstant: 56),
            avatarView.heightAnchor.constraint(equalToConstant: 56),

            avatarLabel.centerXAnchor.constraint(equalTo: avatarView.centerXAnchor),
            avatarLabel.centerYAnchor.constraint(equalTo: avatarView.centerYAnchor),

            nameLabel.topAnchor.constraint(equalTo: avatarView.topAnchor, constant: 4),
            nameLabel.leadingAnchor.constraint(equalTo: avatarView.trailingAnchor, constant: 16),

            conditionLabel.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 4),
            conditionLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),

            statusBadge.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            statusBadge.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            statusBadge.widthAnchor.constraint(greaterThanOrEqualToConstant: 70),
            statusBadge.heightAnchor.constraint(equalToConstant: 24),

            activityLabel.topAnchor.constraint(equalTo: statusBadge.bottomAnchor, constant: 4),
            activityLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
        ])
    }

    func configure(with patient: DoctorDashboardViewController.PatientOverviewItem) {
        nameLabel.text = patient.name
        conditionLabel.text = patient.condition
        activityLabel.text = patient.lastActivity

        // Avatar initials
        let initials = patient.name.split(separator: " ").map { String($0.prefix(1)) }.joined()
        avatarLabel.text = initials

        // Status badge
        statusBadge.text = " \(patient.status.rawValue) "
        switch patient.status {
        case .onTrack:
            statusBadge.textColor = DesignTokens.Colors.primary
            statusBadge.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.1)
        case .needsAttention:
            statusBadge.textColor = DesignTokens.Colors.error
            statusBadge.backgroundColor = DesignTokens.Colors.error.withAlphaComponent(0.1)
        case .inactive:
            statusBadge.textColor = DesignTokens.Colors.neutralDark
            statusBadge.backgroundColor = DesignTokens.Colors.neutralLight
        }
    }
}
