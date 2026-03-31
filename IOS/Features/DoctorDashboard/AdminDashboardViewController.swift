// AdminDashboardViewController.swift
// SR-Cardiocare — Admin Dashboard with doctor cards and system overview
// Shows: doctor cards, patient count, system stats, management options

import UIKit

final class AdminDashboardViewController: UIViewController {

    // MARK: - Models

    struct DoctorOverviewItem {
        let id: String
        let name: String
        let specialty: String
        let patientCount: Int
        let isOnline: Bool
        let rating: Double
    }

    // MARK: - Data

    private var doctors: [DoctorOverviewItem] = [
        DoctorOverviewItem(id: "1", name: "Dr. Sarah Mitchell", specialty: "Orthopedic Rehab", patientCount: 12, isOnline: true, rating: 4.8),
        DoctorOverviewItem(id: "2", name: "Dr. James Wong", specialty: "Sports Medicine", patientCount: 8, isOnline: false, rating: 4.9),
        DoctorOverviewItem(id: "3", name: "Dr. Maria Santos", specialty: "Physical Therapy", patientCount: 15, isOnline: true, rating: 4.7),
        DoctorOverviewItem(id: "4", name: "Dr. Robert Chen", specialty: "Cardiac Rehab", patientCount: 10, isOnline: true, rating: 4.6),
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

    private let adminNameLabel: UILabel = {
        let l = UILabel()
        l.text = "Admin"
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

    private let statsStack: UIStackView = {
        let s = UIStackView()
        s.axis = .horizontal
        s.distribution = .fillEqually
        s.spacing = DesignTokens.Spacing.base
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    private let doctorSectionHeader: UIView = {
        let v = UIView()
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let doctorSectionTitle: UILabel = {
        let l = UILabel()
        l.text = "Doctors"
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let addDoctorButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle("+ Add Doctor", for: .normal)
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

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        // Header
        headerView.addSubviews(welcomeLabel, adminNameLabel, notificationButton)

        NSLayoutConstraint.activate([
            welcomeLabel.topAnchor.constraint(equalTo: headerView.topAnchor, constant: 32),
            welcomeLabel.leadingAnchor.constraint(equalTo: headerView.leadingAnchor, constant: 20),

            adminNameLabel.topAnchor.constraint(equalTo: welcomeLabel.bottomAnchor, constant: 2),
            adminNameLabel.leadingAnchor.constraint(equalTo: welcomeLabel.leadingAnchor),
            adminNameLabel.bottomAnchor.constraint(equalTo: headerView.bottomAnchor, constant: -16),

            notificationButton.topAnchor.constraint(equalTo: welcomeLabel.topAnchor),
            notificationButton.trailingAnchor.constraint(equalTo: headerView.trailingAnchor, constant: -20),
            notificationButton.widthAnchor.constraint(equalToConstant: 40),
            notificationButton.heightAnchor.constraint(equalToConstant: 40),
        ])

        // Stats cards
        let totalDoctors = doctors.count
        let totalPatients = doctors.reduce(0) { $0 + $1.patientCount }
        let onlineDoctors = doctors.filter { $0.isOnline }.count

        let stats: [(String, String, UIColor)] = [
            ("\(totalDoctors)", "Doctors", DesignTokens.Colors.primary),
            ("\(totalPatients)", "Patients", DesignTokens.Colors.success),
            ("\(onlineDoctors)", "Online", DesignTokens.Colors.warning),
        ]

        for (value, label, color) in stats {
            let card = createStatCard(value: value, label: label, color: color)
            statsStack.addArrangedSubview(card)
        }

        // Section header
        doctorSectionHeader.addSubviews(doctorSectionTitle, addDoctorButton)
        NSLayoutConstraint.activate([
            doctorSectionTitle.leadingAnchor.constraint(equalTo: doctorSectionHeader.leadingAnchor, constant: 16),
            doctorSectionTitle.centerYAnchor.constraint(equalTo: doctorSectionHeader.centerYAnchor),
            addDoctorButton.trailingAnchor.constraint(equalTo: doctorSectionHeader.trailingAnchor, constant: -16),
            addDoctorButton.centerYAnchor.constraint(equalTo: doctorSectionHeader.centerYAnchor),
            doctorSectionHeader.heightAnchor.constraint(equalToConstant: 44),
        ])

        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(DoctorOverviewCell.self, forCellReuseIdentifier: DoctorOverviewCell.reuseID)

        view.addSubviews(headerView, statsStack, doctorSectionHeader, tableView)

        NSLayoutConstraint.activate([
            headerView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            headerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),

            statsStack.topAnchor.constraint(equalTo: headerView.bottomAnchor, constant: DesignTokens.Spacing.lg),
            statsStack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            statsStack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),

            doctorSectionHeader.topAnchor.constraint(equalTo: statsStack.bottomAnchor, constant: DesignTokens.Spacing.xl),
            doctorSectionHeader.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            doctorSectionHeader.trailingAnchor.constraint(equalTo: view.trailingAnchor),

            tableView.topAnchor.constraint(equalTo: doctorSectionHeader.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
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
            card.heightAnchor.constraint(equalToConstant: 80),
            valueLbl.centerXAnchor.constraint(equalTo: card.centerXAnchor),
            valueLbl.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            labelLbl.topAnchor.constraint(equalTo: valueLbl.bottomAnchor, constant: 4),
            labelLbl.centerXAnchor.constraint(equalTo: card.centerXAnchor),
        ])

        return card
    }
}

// MARK: - Table View

extension AdminDashboardViewController: UITableViewDataSource, UITableViewDelegate {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { doctors.count }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat { 100 }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: DoctorOverviewCell.reuseID, for: indexPath) as! DoctorOverviewCell
        cell.configure(with: doctors[indexPath.row])
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        // Navigate to doctor detail/management view
    }
}

// MARK: - DoctorOverviewCell

final class DoctorOverviewCell: UITableViewCell {

    static let reuseID = "DoctorOverviewCell"

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
        v.layer.cornerRadius = 30
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

    private let onlineIndicator: UIView = {
        let v = UIView()
        v.backgroundColor = .systemGreen
        v.layer.cornerRadius = 6
        v.layer.borderWidth = 2
        v.layer.borderColor = UIColor.white.cgColor
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let nameLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.body, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let specialtyLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let patientCountLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .medium)
        l.textColor = DesignTokens.Colors.primary
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let ratingLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .medium)
        l.textColor = DesignTokens.Colors.warning
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
        card.addSubviews(avatarView, nameLabel, specialtyLabel, patientCountLabel, ratingLabel, onlineIndicator)
        avatarView.addSubview(avatarLabel)

        NSLayoutConstraint.activate([
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 4),
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -4),

            avatarView.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            avatarView.centerYAnchor.constraint(equalTo: card.centerYAnchor),
            avatarView.widthAnchor.constraint(equalToConstant: 60),
            avatarView.heightAnchor.constraint(equalToConstant: 60),

            avatarLabel.centerXAnchor.constraint(equalTo: avatarView.centerXAnchor),
            avatarLabel.centerYAnchor.constraint(equalTo: avatarView.centerYAnchor),

            onlineIndicator.bottomAnchor.constraint(equalTo: avatarView.bottomAnchor),
            onlineIndicator.trailingAnchor.constraint(equalTo: avatarView.trailingAnchor),
            onlineIndicator.widthAnchor.constraint(equalToConstant: 12),
            onlineIndicator.heightAnchor.constraint(equalToConstant: 12),

            nameLabel.topAnchor.constraint(equalTo: avatarView.topAnchor, constant: 4),
            nameLabel.leadingAnchor.constraint(equalTo: avatarView.trailingAnchor, constant: 16),

            specialtyLabel.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 4),
            specialtyLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),

            patientCountLabel.topAnchor.constraint(equalTo: specialtyLabel.bottomAnchor, constant: 8),
            patientCountLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),

            ratingLabel.centerYAnchor.constraint(equalTo: card.centerYAnchor),
            ratingLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
        ])
    }

    func configure(with doctor: AdminDashboardViewController.DoctorOverviewItem) {
        nameLabel.text = doctor.name
        specialtyLabel.text = doctor.specialty
        patientCountLabel.text = "\(doctor.patientCount) patients"
        ratingLabel.text = "★ \(doctor.rating)"
        onlineIndicator.isHidden = !doctor.isOnline

        // Avatar initials (skip "Dr.")
        let nameParts = doctor.name.replacingOccurrences(of: "Dr. ", with: "").split(separator: " ")
        let initials = nameParts.map { String($0.prefix(1)) }.joined()
        avatarLabel.text = initials
    }
}
