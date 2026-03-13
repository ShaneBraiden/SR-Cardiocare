// NotificationsViewController.swift
// SR-Cardiocare — Notification Settings
// Stitch Screen: bd99eb12 (24_notifications)
// Shows: grouped toggle settings for exercise reminders, doctor comms, progress

import UIKit

final class NotificationsViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {

    // MARK: - Data

    private struct SettingItem {
        let icon: String          // SF Symbol name
        let iconBg: UIColor
        let iconTint: UIColor
        let title: String
        let subtitle: String?
        let isToggle: Bool        // true = switch, false = disclosure
        var isOn: Bool
    }

    private struct SettingSection {
        let title: String
        let footer: String?
        var items: [SettingItem]
    }

    private var sections: [SettingSection] = [
        SettingSection(title: "REHABILITATION & GOALS", footer: "Receive gentle nudges to keep your recovery on track.", items: [
            SettingItem(icon: "figure.strengthtraining.traditional", iconBg: DesignTokens.Colors.primary.withAlphaComponent(0.1), iconTint: DesignTokens.Colors.primary, title: "Daily Exercise Alerts", subtitle: nil, isToggle: true, isOn: true),
            SettingItem(icon: "clock", iconBg: DesignTokens.Colors.primary.withAlphaComponent(0.1), iconTint: DesignTokens.Colors.primary, title: "Reminder Time", subtitle: "09:00 AM", isToggle: false, isOn: false),
            SettingItem(icon: "exclamationmark.triangle", iconBg: DesignTokens.Colors.warning.withAlphaComponent(0.12), iconTint: DesignTokens.Colors.warning, title: "Missed Session Alerts", subtitle: nil, isToggle: true, isOn: true),
        ]),
        SettingSection(title: "DOCTOR & CLINIC", footer: nil, items: [
            SettingItem(icon: "message.fill", iconBg: DesignTokens.Colors.primaryDark.withAlphaComponent(0.12), iconTint: DesignTokens.Colors.primaryDark, title: "New Messages", subtitle: "From Dr. Sarah Jenkins", isToggle: true, isOn: true),
            SettingItem(icon: "calendar", iconBg: DesignTokens.Colors.chartSecondaryTeal.withAlphaComponent(0.15), iconTint: DesignTokens.Colors.primaryDark, title: "Appointment Reminders", subtitle: nil, isToggle: true, isOn: true),
        ]),
        SettingSection(title: "PROGRESS & ACHIEVEMENTS", footer: nil, items: [
            SettingItem(icon: "chart.bar.fill", iconBg: DesignTokens.Colors.success.withAlphaComponent(0.12), iconTint: DesignTokens.Colors.success, title: "Weekly Progress Summary", subtitle: nil, isToggle: true, isOn: false),
            SettingItem(icon: "medal.fill", iconBg: DesignTokens.Colors.warning.withAlphaComponent(0.12), iconTint: DesignTokens.Colors.warning, title: "Achievement Badges", subtitle: nil, isToggle: true, isOn: true),
        ]),
    ]

    // MARK: - UI

    private let tableView: UITableView = {
        let tv = UITableView(frame: .zero, style: .insetGrouped)
        tv.backgroundColor = .clear
        tv.separatorInset = UIEdgeInsets(top: 0, left: 56, bottom: 0, right: 0)
        tv.translatesAutoresizingMaskIntoConstraints = false
        return tv
    }()

    // Custom header
    private let headerView: UIView = {
        let v = UIView()
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let backButton: UIButton = {
        let b = UIButton(type: .system)
        let config = UIImage.SymbolConfiguration(pointSize: 20, weight: .medium)
        b.setImage(UIImage(systemName: "arrow.left", withConfiguration: config), for: .normal)
        b.tintColor = DesignTokens.Colors.textMain
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let headerTitle: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("notifications_title", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
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

        // Header bar
        let headerBar = UIView()
        headerBar.backgroundColor = DesignTokens.Colors.backgroundLight
        headerBar.translatesAutoresizingMaskIntoConstraints = false
        let separator = UIView()
        separator.backgroundColor = DesignTokens.Colors.neutralLight
        separator.translatesAutoresizingMaskIntoConstraints = false
        headerBar.addSubviews(backButton, headerTitle, separator)

        view.addSubviews(headerBar, tableView)

        NSLayoutConstraint.activate([
            headerBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            headerBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            headerBar.heightAnchor.constraint(equalToConstant: 52),

            backButton.leadingAnchor.constraint(equalTo: headerBar.leadingAnchor, constant: DesignTokens.Spacing.base),
            backButton.centerYAnchor.constraint(equalTo: headerBar.centerYAnchor),
            backButton.widthAnchor.constraint(equalToConstant: 40),
            backButton.heightAnchor.constraint(equalToConstant: 40),

            headerTitle.centerXAnchor.constraint(equalTo: headerBar.centerXAnchor),
            headerTitle.centerYAnchor.constraint(equalTo: headerBar.centerYAnchor),

            separator.leadingAnchor.constraint(equalTo: headerBar.leadingAnchor),
            separator.trailingAnchor.constraint(equalTo: headerBar.trailingAnchor),
            separator.bottomAnchor.constraint(equalTo: headerBar.bottomAnchor),
            separator.heightAnchor.constraint(equalToConstant: 0.5),

            tableView.topAnchor.constraint(equalTo: headerBar.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        backButton.addTarget(self, action: #selector(didTapBack), for: .touchUpInside)

        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(NotificationSettingCell.self, forCellReuseIdentifier: NotificationSettingCell.id)
    }

    // MARK: - Actions

    @objc private func didTapBack() {
        navigationController?.popViewController(animated: true)
    }

    @objc private func toggleChanged(_ sender: UISwitch) {
        let section = sender.tag / 100
        let row = sender.tag % 100
        guard section < sections.count, row < sections[section].items.count else { return }
        sections[section].items[row].isOn = sender.isOn
    }

    // MARK: - TableView

    func numberOfSections(in tableView: UITableView) -> Int { sections.count }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { sections[section].items.count }

    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? { sections[section].title }

    func tableView(_ tableView: UITableView, titleForFooterInSection section: Int) -> String? { sections[section].footer }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: NotificationSettingCell.id, for: indexPath) as! NotificationSettingCell
        let item = sections[indexPath.section].items[indexPath.row]
        cell.configure(with: item)
        if item.isToggle {
            let toggle = UISwitch()
            toggle.isOn = item.isOn
            toggle.onTintColor = DesignTokens.Colors.primary
            toggle.tag = indexPath.section * 100 + indexPath.row
            toggle.addTarget(self, action: #selector(toggleChanged(_:)), for: .valueChanged)
            cell.accessoryView = toggle
            cell.selectionStyle = .none
        } else {
            cell.accessoryView = nil
            let badge = UILabel()
            badge.text = item.subtitle
            badge.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
            badge.textColor = DesignTokens.Colors.primary
            badge.backgroundColor = DesignTokens.Colors.neutralLight
            badge.textAlignment = .center
            badge.layer.cornerRadius = 6
            badge.clipsToBounds = true
            badge.sizeToFit()
            badge.frame.size = CGSize(width: badge.frame.width + 16, height: 28)
            cell.accessoryView = badge
            cell.selectionStyle = .default
        }
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let item = sections[indexPath.section].items[indexPath.row]
        if !item.isToggle {
            // Would present time picker
        }
    }

    func tableView(_ tableView: UITableView, willDisplayHeaderView view: UIView, forSection section: Int) {
        guard let header = view as? UITableViewHeaderFooterView else { return }
        header.textLabel?.font = DesignTokens.Typography.inter(11, weight: .semibold)
        header.textLabel?.textColor = DesignTokens.Colors.textSub
    }
}

// MARK: - NotificationSettingCell

private final class NotificationSettingCell: UITableViewCell {
    static let id = "NotificationSettingCell"

    private let iconContainer: UIView = {
        let v = UIView()
        v.layer.cornerRadius = 16
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let iconImageView: UIImageView = {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFit
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    private let titleLbl: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.body, weight: .medium)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let subtitleLbl: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupCell()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    private func setupCell() {
        backgroundColor = DesignTokens.Colors.surfaceLight
        contentView.addSubviews(iconContainer, titleLbl, subtitleLbl)
        iconContainer.addSubview(iconImageView)

        NSLayoutConstraint.activate([
            iconContainer.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            iconContainer.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            iconContainer.widthAnchor.constraint(equalToConstant: 32),
            iconContainer.heightAnchor.constraint(equalToConstant: 32),

            iconImageView.centerXAnchor.constraint(equalTo: iconContainer.centerXAnchor),
            iconImageView.centerYAnchor.constraint(equalTo: iconContainer.centerYAnchor),
            iconImageView.widthAnchor.constraint(equalToConstant: 18),
            iconImageView.heightAnchor.constraint(equalToConstant: 18),

            titleLbl.leadingAnchor.constraint(equalTo: iconContainer.trailingAnchor, constant: 12),
            titleLbl.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            titleLbl.trailingAnchor.constraint(lessThanOrEqualTo: contentView.trailingAnchor, constant: -80),

            subtitleLbl.leadingAnchor.constraint(equalTo: titleLbl.leadingAnchor),
            subtitleLbl.topAnchor.constraint(equalTo: titleLbl.bottomAnchor, constant: 2),
            subtitleLbl.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -12),
        ])
    }

    func configure(with item: NotificationsViewController.SettingItem) {
        titleLbl.text = item.title
        iconContainer.backgroundColor = item.iconBg
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .medium)
        iconImageView.image = UIImage(systemName: item.icon, withConfiguration: config)
        iconImageView.tintColor = item.iconTint

        if let sub = item.subtitle, item.isToggle {
            subtitleLbl.text = sub
            subtitleLbl.isHidden = false
        } else if !item.isToggle {
            subtitleLbl.isHidden = true
        } else {
            subtitleLbl.isHidden = true
        }
    }
}

// Make SettingItem accessible to cell
extension NotificationsViewController {
    typealias SettingItem = NotificationsViewController.SettingItem
}
