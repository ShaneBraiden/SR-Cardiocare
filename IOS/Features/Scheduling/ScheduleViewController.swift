// ScheduleViewController.swift
// SR-Cardiocare — Bidirectional Schedule (Doctor + Patient)
// Stitch Screens: 64a47365 (11_schedule_v1), 71c01fcd (14_schedule_v2), aba16fef (19_schedule_v3)
// Shows: horizontal date picker, daily appointment list, FAB to add appointment
// Uses Lexend font per Stitch screen

import UIKit

final class ScheduleViewController: UIViewController {

    // MARK: - Models

    struct ScheduleItem {
        let time: String
        let patientName: String
        let type: String
        let status: AppointmentStatus
    }

    enum AppointmentStatus: String {
        case confirmed = "Confirmed"
        case pending = "Pending"
        case cancelled = "Cancelled"
    }

    // MARK: - Data

    private var selectedDayIndex = 2 // Wednesday (today)
    private let weekDays: [(day: String, date: String)] = [
        ("Mon", "16"), ("Tue", "17"), ("Wed", "18"), ("Thu", "19"), ("Fri", "20"), ("Sat", "21"), ("Sun", "22")
    ]

    private let appointments: [ScheduleItem] = [
        ScheduleItem(time: "9:00 AM", patientName: "Sarah Wilson", type: "Follow-up", status: .confirmed),
        ScheduleItem(time: "10:30 AM", patientName: "James Chen", type: "Assessment", status: .confirmed),
        ScheduleItem(time: "12:00 PM", patientName: "Lunch Break", type: "", status: .pending),
        ScheduleItem(time: "1:30 PM", patientName: "Maria Garcia", type: "Progress Review", status: .pending),
        ScheduleItem(time: "3:00 PM", patientName: "David Johnson", type: "Initial Consultation", status: .confirmed),
        ScheduleItem(time: "4:30 PM", patientName: "Emily Brown", type: "Rehab Session", status: .cancelled),
    ]

    // MARK: - UI

    private let headerView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let titleLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("schedule_title", comment: "")
        l.font = DesignTokens.Typography.lexend(DesignTokens.Typography.title, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let monthLabel: UILabel = {
        let l = UILabel()
        l.text = "October 2023"
        l.font = DesignTokens.Typography.lexend(DesignTokens.Typography.subheadline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let todayButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("today", comment: ""), for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.lexend(DesignTokens.Typography.caption, weight: .medium)
        b.setTitleColor(DesignTokens.Colors.primary, for: .normal)
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private lazy var dayCollectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.scrollDirection = .horizontal
        layout.minimumInteritemSpacing = 8
        layout.sectionInset = UIEdgeInsets(top: 0, left: 16, bottom: 0, right: 16)
        let cv = UICollectionView(frame: .zero, collectionViewLayout: layout)
        cv.backgroundColor = .clear
        cv.showsHorizontalScrollIndicator = false
        cv.dataSource = self
        cv.delegate = self
        cv.register(DayCell.self, forCellWithReuseIdentifier: DayCell.reuseID)
        cv.translatesAutoresizingMaskIntoConstraints = false
        return cv
    }()

    private let tableView: UITableView = {
        let tv = UITableView(frame: .zero, style: .plain)
        tv.separatorStyle = .none
        tv.backgroundColor = .clear
        tv.translatesAutoresizingMaskIntoConstraints = false
        return tv
    }()

    private let addFAB: UIButton = {
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
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(AppointmentCell.self, forCellReuseIdentifier: AppointmentCell.reuseID)
    }

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        // Header
        let monthRow = UIView()
        monthRow.translatesAutoresizingMaskIntoConstraints = false
        monthRow.addSubviews(monthLabel, todayButton)
        NSLayoutConstraint.activate([
            monthLabel.leadingAnchor.constraint(equalTo: monthRow.leadingAnchor),
            monthLabel.centerYAnchor.constraint(equalTo: monthRow.centerYAnchor),
            todayButton.trailingAnchor.constraint(equalTo: monthRow.trailingAnchor),
            todayButton.centerYAnchor.constraint(equalTo: monthRow.centerYAnchor),
            monthRow.heightAnchor.constraint(equalToConstant: 32),
        ])

        headerView.addSubviews(titleLabel, monthRow, dayCollectionView)
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: headerView.topAnchor, constant: 12),
            titleLabel.leadingAnchor.constraint(equalTo: headerView.leadingAnchor, constant: 16),

            monthRow.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 16),
            monthRow.leadingAnchor.constraint(equalTo: headerView.leadingAnchor, constant: 16),
            monthRow.trailingAnchor.constraint(equalTo: headerView.trailingAnchor, constant: -16),

            dayCollectionView.topAnchor.constraint(equalTo: monthRow.bottomAnchor, constant: 12),
            dayCollectionView.leadingAnchor.constraint(equalTo: headerView.leadingAnchor),
            dayCollectionView.trailingAnchor.constraint(equalTo: headerView.trailingAnchor),
            dayCollectionView.heightAnchor.constraint(equalToConstant: 80),
            dayCollectionView.bottomAnchor.constraint(equalTo: headerView.bottomAnchor, constant: -8),
        ])

        view.addSubviews(headerView, tableView, addFAB)

        NSLayoutConstraint.activate([
            headerView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            headerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),

            tableView.topAnchor.constraint(equalTo: headerView.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            addFAB.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            addFAB.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            addFAB.widthAnchor.constraint(equalToConstant: 56),
            addFAB.heightAnchor.constraint(equalToConstant: 56),
        ])
    }
}

// MARK: - Day Collection View

extension ScheduleViewController: UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int { weekDays.count }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: DayCell.reuseID, for: indexPath) as! DayCell
        let wd = weekDays[indexPath.item]
        cell.configure(day: wd.day, date: wd.date, isSelected: indexPath.item == selectedDayIndex)
        return cell
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        CGSize(width: 56, height: 72)
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        selectedDayIndex = indexPath.item
        collectionView.reloadData()
    }
}

// MARK: - Appointments Table View

extension ScheduleViewController: UITableViewDataSource, UITableViewDelegate {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { appointments.count }
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat { 80 }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: AppointmentCell.reuseID, for: indexPath) as! AppointmentCell
        cell.configure(with: appointments[indexPath.row])
        return cell
    }
}

// MARK: - DayCell

final class DayCell: UICollectionViewCell {
    static let reuseID = "DayCell"

    private let dayLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.lexend(DesignTokens.Typography.caption, weight: .medium)
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let dateLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.lexend(DesignTokens.Typography.body, weight: .bold)
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let dotView: UIView = {
        let v = UIView()
        v.layer.cornerRadius = 2
        v.translatesAutoresizingMaskIntoConstraints = false
        v.isHidden = true
        return v
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.layer.cornerRadius = DesignTokens.Radius.xl
        contentView.addSubviews(dayLabel, dateLabel, dotView)
        NSLayoutConstraint.activate([
            dayLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 10),
            dayLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            dateLabel.topAnchor.constraint(equalTo: dayLabel.bottomAnchor, constant: 4),
            dateLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            dotView.topAnchor.constraint(equalTo: dateLabel.bottomAnchor, constant: 4),
            dotView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            dotView.widthAnchor.constraint(equalToConstant: 4),
            dotView.heightAnchor.constraint(equalToConstant: 4),
        ])
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(day: String, date: String, isSelected: Bool) {
        dayLabel.text = day
        dateLabel.text = date
        if isSelected {
            contentView.backgroundColor = DesignTokens.Colors.primary
            dayLabel.textColor = .white
            dateLabel.textColor = .white
            dotView.backgroundColor = .white
            dotView.isHidden = false
            contentView.layer.shadowColor = DesignTokens.Colors.primary.cgColor
            contentView.layer.shadowOpacity = 0.3
            contentView.layer.shadowRadius = 8
        } else {
            contentView.backgroundColor = .clear
            dayLabel.textColor = DesignTokens.Colors.textSub
            dateLabel.textColor = DesignTokens.Colors.textMain
            dotView.isHidden = true
            contentView.layer.shadowOpacity = 0
        }
    }
}

// MARK: - AppointmentCell

final class AppointmentCell: UITableViewCell {
    static let reuseID = "AppointmentCell"

    private let timeLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.lexend(DesignTokens.Typography.subheadline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let card: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.layer.cornerRadius = DesignTokens.Radius.xl
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let nameLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.lexend(DesignTokens.Typography.body, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let typeLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.lexend(DesignTokens.Typography.caption)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let statusDot: UIView = {
        let v = UIView()
        v.layer.cornerRadius = 4
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .clear
        selectionStyle = .none
        contentView.addSubviews(timeLabel, card)
        card.addSubviews(statusDot, nameLabel, typeLabel)

        NSLayoutConstraint.activate([
            timeLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            timeLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            timeLabel.widthAnchor.constraint(equalToConstant: 72),

            card.leadingAnchor.constraint(equalTo: timeLabel.trailingAnchor, constant: 8),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 4),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -4),

            statusDot.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 12),
            statusDot.centerYAnchor.constraint(equalTo: card.centerYAnchor),
            statusDot.widthAnchor.constraint(equalToConstant: 8),
            statusDot.heightAnchor.constraint(equalToConstant: 8),

            nameLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            nameLabel.leadingAnchor.constraint(equalTo: statusDot.trailingAnchor, constant: 8),
            nameLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -12),

            typeLabel.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 2),
            typeLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),
        ])
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(with item: ScheduleViewController.ScheduleItem) {
        timeLabel.text = item.time
        nameLabel.text = item.patientName
        typeLabel.text = item.type

        switch item.status {
        case .confirmed: statusDot.backgroundColor = DesignTokens.Colors.success
        case .pending: statusDot.backgroundColor = DesignTokens.Colors.warning
        case .cancelled: statusDot.backgroundColor = DesignTokens.Colors.error
        }
    }
}
