const API_BASE = '/api/payroll';
let currentSchedules = {};
let editingScheduleId = null;
// Format money to VND
const formatMoney = (amount) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

// ==========================================
// 0. INITIALIZATION
// ==========================================
async function loadLocations() {
    try {
        const res = await fetch(`${API_BASE}/locations`);
        if (!res.ok) return;
        const locations = await res.json();
        const select = document.getElementById('locationId');
        if (locations.length === 0) {
            select.innerHTML = '<option value="">Chưa có địa điểm</option>';
            return;
        }
        select.innerHTML = locations.map(loc => `<option value="${loc.id}">${loc.name}</option>`).join('');
    } catch (e) {
        console.error('Lỗi khi tải địa điểm', e);
    }
}

async function loadEmployees() {
    try {
        const res = await fetch(`${API_BASE}/employees`);
        if (!res.ok) return;
        const employees = await res.json();
        const datalist = document.getElementById('employeeSuggestions');
        datalist.innerHTML = employees.map(emp => `<option value="${emp.id}">${emp.fullName} - ${emp.phoneNumber || 'Không có SĐT'}</option>`).join('');
    } catch (e) {
        console.error('Lỗi khi tải danh sách nhân viên', e);
    }
}

// Khởi tạo ngày tháng mặc định là tháng hiện tại
const now = new Date();
const currentMonth = now.getMonth() + 1;
const currentYear = now.getFullYear();

['overviewMonth', 'empMonth', 'month'].forEach(id => {
    if(document.getElementById(id)) document.getElementById(id).value = currentMonth;
});
['overviewYear', 'empYear', 'year'].forEach(id => {
    if(document.getElementById(id)) document.getElementById(id).value = currentYear;
});

loadLocations();
loadEmployees();

// ==========================================
// REFRESH ALL DATA LOGIC
// ==========================================
window.refreshAllData = function() {
    loadLocations();
    loadEmployees();
    loadAdvanceRequests();
    
    // Refresh Overview
    if (document.getElementById('overviewMonth') && document.getElementById('overviewMonth').value) {
        document.getElementById('overviewForm').dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
    }
    // Refresh Employee list
    if (document.getElementById('empMonth') && document.getElementById('empMonth').value) {
        document.getElementById('employeesForm').dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
    }
    // Refresh Payroll details
    if (document.getElementById('empId') && document.getElementById('empId').value) {
        document.getElementById('payrollForm').dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
    }
};

// ==========================================
// NAVIGATION LOGIC
// ==========================================
const navItems = document.querySelectorAll('.nav-item');
const pages = document.querySelectorAll('.page-section');

navItems.forEach(item => {
    item.addEventListener('click', () => {
        // Remove active class from all nav items
        navItems.forEach(nav => nav.classList.remove('active'));
        // Add active class to clicked nav
        item.classList.add('active');

        // Hide all pages
        pages.forEach(page => page.classList.remove('active'));
        // Show target page
        const targetId = item.getAttribute('data-target');
        document.getElementById(targetId).classList.add('active');
    });
});


// ==========================================
// 1. TỔNG QUAN THÁNG
// ==========================================
document.getElementById('overviewForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const month = document.getElementById('overviewMonth').value;
    const year = document.getElementById('overviewYear').value;
    const resultBox = document.getElementById('overviewResult');
    
    try {
        const res = await fetch(`${API_BASE}/reports/monthly-overview?month=${month}&year=${year}`);
        if (!res.ok) throw new Error('Không tìm thấy dữ liệu hoặc có lỗi xảy ra');
        const data = await res.json();
        
        let schedulesHtml = '';
        if (data.schedules && data.schedules.length > 0) {
            let currentDate = '';
            let isEvenDay = false;
            
            schedulesHtml = `
                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 2rem; margin-bottom: 1rem;">
                    <h3 style="margin: 0;">Danh sách các ca trong tháng</h3>
                    <button type="button" class="btn-text" style="color: var(--primary); font-weight: bold; border: 1px solid var(--primary); border-radius: 6px; padding: 6px 12px;" onclick="document.getElementById('overviewForm').dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }))">
                        🔄 Làm mới dữ liệu
                    </button>
                </div>
                <table class="dates-table">
                    <thead>
                        <tr>
                            <th>ID Ca</th>
                            <th>Ngày</th>
                            <th>Ca</th>
                            <th>Địa điểm</th>
                            <th class="text-right">Lương cố định</th>
                            <th class="text-center">Số NV ngoài</th>
                            <th class="text-right">Lương ngoài</th>
                            <th class="text-right">Doanh thu</th>
                            <th class="text-right">Tiền cắt</th>
                            <th class="text-center">Trạng thái</th>
                            <th class="text-center">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${data.schedules.map(s => {
                            currentSchedules[s.id] = s; // Lưu vào biến toàn cục
                            if (s.workDate !== currentDate) {
                                currentDate = s.workDate;
                                isEvenDay = !isEvenDay;
                            }
                            const rowClass = isEvenDay ? 'alt-row' : '';
                            
                            const shiftColors = {
                                'SÁNG': 'background: rgba(250, 204, 21, 0.2); color: #fde047;',
                                'CHIỀU': 'background: rgba(56, 189, 248, 0.2); color: #7dd3fc;',
                                'TỐI': 'background: rgba(168, 85, 247, 0.2); color: #d8b4fe;'
                            };
                            
                            const locationColors = {
                                'Cát Quế': 'background: rgba(139, 92, 246, 0.2); color: #a78bfa;',
                                'Thực Dung': 'background: rgba(20, 184, 166, 0.2); color: #5eead4;',
                                'Yên Sở': 'background: rgba(249, 115, 22, 0.2); color: #fdba74;'
                            };
                            
                            const shiftStyle = shiftColors[s.shift] || 'background: rgba(156, 163, 175, 0.2); color: #d1d5db;';
                            const locStyle = s.locationName ? (locationColors[s.locationName] || 'background: rgba(156, 163, 175, 0.2); color: #d1d5db;') : '';
                            
                            const locHtml = s.locationName ? `<span style="${locStyle} padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600;">${s.locationName}</span>` : '';
                            
                            return `
                            <tr class="${rowClass}">
                                <td>#${s.id}</td>
                                <td>${s.workDate}</td>
                                <td><span style="${shiftStyle} padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600;">${s.shift}</span></td>
                                <td>${locHtml}</td>
                                <td class="format-money text-right" style="color: #818cf8">${formatMoney(s.luongNhanVien)}</td>
                                <td class="text-center" style="font-weight: bold;">${s.casualWorkerCount || 0}</td>
                                <td class="format-money text-right" style="color: #fb7185">${formatMoney(s.casualWage)}</td>
                                <td class="format-money text-right" style="font-weight: 500;">${formatMoney(s.thanhTien)}</td>
                                <td class="format-money text-right" style="color: var(--success); font-weight: bold;">${formatMoney(s.tienCat)}</td>
                                <td class="text-center">
                                    <select class="status-select" data-id="${s.id}" style="padding: 4px; border-radius: 4px; border: 1px solid var(--border-color); background: rgba(0,0,0,0.5); font-weight: bold; color: ${s.paymentStatus === 'ĐÃ NHẬN' ? 'var(--success)' : 'var(--danger)'}">
                                        <option value="CHƯA NHẬN" ${s.paymentStatus === 'CHƯA NHẬN' ? 'selected' : ''} style="color: var(--danger)">CHƯA NHẬN</option>
                                        <option value="ĐÃ NHẬN" ${s.paymentStatus === 'ĐÃ NHẬN' ? 'selected' : ''} style="color: var(--success)">ĐÃ NHẬN</option>
                                    </select>
                                </td>
                                <td>
                                    <button type="button" class="btn-text" style="color: var(--primary); font-weight: bold; padding: 4px 8px; border: 1px solid var(--primary); border-radius: 4px;" onclick="showScheduleDetails(${s.id})">Xem</button>
                                    <button type="button" class="btn-text" style="color: #eab308; font-weight: bold; padding: 4px 8px; border: 1px solid #eab308; border-radius: 4px; margin-left: 4px;" onclick="editWorkSchedule(${s.id})">Sửa</button>
                                    <button type="button" class="btn-text" style="color: var(--danger); font-weight: bold; padding: 4px 8px; border: 1px solid var(--danger); border-radius: 4px; margin-left: 4px;" onclick="deleteWorkSchedule(${s.id})">Xóa</button>
                                </td>
                            </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            `;
        } else {
            schedulesHtml = '<p style="color: var(--text-secondary); font-size: 0.875rem; margin-top: 1rem;">Chưa có ca làm việc nào trong tháng này.</p>';
        }

        resultBox.innerHTML = `
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1.5rem; margin-bottom: 1rem;">
                <div style="background: rgba(168,85,247,0.1); padding: 1.5rem; border-radius: 0.75rem; border: 1px solid rgba(168,85,247,0.3);">
                    <div style="font-size: 0.95rem; color: var(--text-secondary); margin-bottom: 0.5rem;">Tổng Tiền Đám</div>
                    <div style="font-size: 1.5rem; font-weight: bold; color: #a855f7" class="format-money">${formatMoney(data.tongTienDam)}</div>
                </div>
                <div style="background: rgba(99,102,241,0.1); padding: 1.5rem; border-radius: 0.75rem; border: 1px solid rgba(99,102,241,0.3);">
                    <div style="font-size: 0.95rem; color: var(--text-secondary); margin-bottom: 0.5rem;">Lương NV (Ghi sổ)</div>
                    <div style="font-size: 1.5rem; font-weight: bold; color: #6366f1" class="format-money">${formatMoney(data.tongTienTraNhanVien)}</div>
                </div>
                <div style="background: rgba(16,185,129,0.1); padding: 1.5rem; border-radius: 0.75rem; border: 1px solid rgba(16,185,129,0.3);">
                    <div style="font-size: 0.95rem; color: var(--text-secondary); margin-bottom: 0.5rem;">Tổng Tiền Cắt</div>
                    <div style="font-size: 1.5rem; font-weight: bold; color: var(--success)" class="format-money">${formatMoney(data.tongTienCat)}</div>
                </div>
                <div style="background: rgba(239,68,68,0.1); padding: 1.5rem; border-radius: 0.75rem; border: 1px solid rgba(239,68,68,0.3);">
                    <div style="font-size: 0.95rem; color: var(--text-secondary); margin-bottom: 0.5rem;">Lương Thuê Ngoài</div>
                    <div style="font-size: 1.5rem; font-weight: bold; color: #f43f5e" class="format-money">${formatMoney(data.tongLuongThueNgoai)}</div>
                </div>
                
                <!-- NEW METRICS -->
                <div style="background: rgba(245,158,11,0.1); padding: 1.5rem; border-radius: 0.75rem; border: 1px solid rgba(245,158,11,0.3);">
                    <div style="font-size: 0.95rem; color: var(--text-secondary); margin-bottom: 0.5rem;">Công Nợ (Khách Chưa Trả)</div>
                    <div style="font-size: 1.5rem; font-weight: bold; color: #f59e0b" class="format-money">${formatMoney(data.congNo)}</div>
                </div>
                <div style="background: rgba(56,189,248,0.1); padding: 1.5rem; border-radius: 0.75rem; border: 1px solid rgba(56,189,248,0.3);">
                    <div style="font-size: 0.95rem; color: var(--text-secondary); margin-bottom: 0.5rem;">Tiền Đã Nhận</div>
                    <div style="font-size: 1.5rem; font-weight: bold; color: #38bdf8" class="format-money">${formatMoney(data.soTienDaNhan)}</div>
                </div>
                <div style="background: rgba(236,72,153,0.1); padding: 1.5rem; border-radius: 0.75rem; border: 1px solid rgba(236,72,153,0.3);">
                    <div style="font-size: 0.95rem; color: var(--text-secondary); margin-bottom: 0.5rem;">Đã Ứng/Trả NV Cố Định</div>
                    <div style="font-size: 1.5rem; font-weight: bold; color: #ec4899" class="format-money">${formatMoney(data.tongDaTraNhanVien)}</div>
                </div>
                <div style="background: rgba(16,185,129,0.2); padding: 1.5rem; border-radius: 0.75rem; border: 2px solid rgba(16,185,129,0.6); grid-column: 1 / -1;">
                    <div style="font-size: 1.1rem; color: var(--text-primary); margin-bottom: 0.5rem; font-weight: bold;">TỒN CUỐI KỲ (Tiền Thực Tế Trong Túi)</div>
                    <div style="font-size: 2rem; font-weight: bold; color: var(--success)" class="format-money">${formatMoney(data.tonCuoiKi)}</div>
                    <div style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 0.5rem;">= (Tiền Đã Nhận) - (Lương Thuê Ngoài + Đã Ứng/Trả NV Cố Định)</div>
                </div>
            </div>
            ${schedulesHtml}
        `;
        resultBox.classList.remove('hidden');

        // Gắn sự kiện thay đổi trạng thái
        document.querySelectorAll('.status-select').forEach(selectEl => {
            selectEl.addEventListener('change', async (event) => {
                const scheduleId = event.target.getAttribute('data-id');
                const newStatus = event.target.value;
                try {
                    const updateRes = await fetch(`${API_BASE}/work-schedules/${scheduleId}/status?status=${newStatus}`, { method: 'PUT' });
                    if (!updateRes.ok) throw new Error('Không thể cập nhật trạng thái');
                    
                    // Chỉ đổi màu chữ trên UI, KHÔNG reload lại toàn bộ trang
                    if (newStatus === 'ĐÃ NHẬN') {
                        event.target.style.color = 'var(--success)';
                    } else {
                        event.target.style.color = 'var(--danger)';
                    }
                } catch (err) {
                    alert(err.message);
                    // Đảo ngược lại giá trị nếu lỗi
                    event.target.value = newStatus === 'ĐÃ NHẬN' ? 'CHƯA NHẬN' : 'ĐÃ NHẬN';
                }
            });
        });

    } catch (error) {
        resultBox.innerHTML = `<p style="color: var(--danger)">${error.message}</p>`;
        resultBox.classList.remove('hidden');
    }
});


// ==========================================
// 1.5. DANH SÁCH TẤT CẢ NHÂN VIÊN
// ==========================================
document.getElementById('employeesForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const month = document.getElementById('empMonth').value;
    const year = document.getElementById('empYear').value;
    const resultBox = document.getElementById('employeesResult');
    
    try {
        const res = await fetch(`${API_BASE}/reports/employees?month=${month}&year=${year}`);
        if (!res.ok) throw new Error('Lỗi khi tải danh sách nhân viên');
        const data = await res.json();
        
        if (data.length === 0) {
            resultBox.innerHTML = '<p style="color: var(--text-secondary);">Chưa có nhân viên nào trong hệ thống.</p>';
        } else {
            let totalTongLuong = 0;
            let totalDaUng = 0;
            let totalThucNhan = 0;
            
            const tbodyHtml = data.map(emp => {
                totalTongLuong += emp.totalWage;
                totalDaUng += emp.totalAdvance;
                totalThucNhan += emp.actualReceived;
                
                return `
                <tr>
                    <td>#${emp.id}</td>
                    <td style="font-weight: bold;">${emp.fullName}</td>
                    <td class="format-money" style="color: #6366f1">${formatMoney(emp.totalWage)}</td>
                    <td class="format-money" style="color: var(--danger)">${formatMoney(emp.totalAdvance)}</td>
                    <td class="format-money" style="color: ${emp.actualReceived < 0 ? 'var(--danger)' : 'var(--success)'}; font-weight: bold;">
                        ${formatMoney(emp.actualReceived)}
                    </td>
                    <td>
                        <button type="button" class="btn-text" style="color: var(--primary); font-weight: bold; padding: 4px 8px; border: 1px solid var(--primary); border-radius: 4px;" onclick="
                            document.querySelector('.nav-item[data-target=\\'payroll-page\\']').click();
                            document.getElementById('empId').value = '${emp.id}';
                            document.getElementById('month').value = ${month};
                            document.getElementById('year').value = ${year};
                            document.getElementById('payrollForm').dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
                        ">Chi tiết</button>
                    </td>
                </tr>
                `;
            }).join('');
            
            resultBox.innerHTML = `
                <table class="dates-table" style="margin-top: 1rem;">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Họ Tên</th>
                            <th>Tổng Lương (Ghi Sổ)</th>
                            <th>Đã Ứng / Trả</th>
                            <th>Thực Nhận (Còn Lại)</th>
                            <th>Thao Tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${tbodyHtml}
                    </tbody>
                    <tfoot style="background: rgba(255,255,255,0.05); font-weight: bold;">
                        <tr>
                            <td colspan="2" style="text-align: right; padding: 1rem;">TỔNG CỘNG TẤT CẢ NV:</td>
                            <td class="format-money" style="color: #6366f1">${formatMoney(totalTongLuong)}</td>
                            <td class="format-money" style="color: var(--danger)">${formatMoney(totalDaUng)}</td>
                            <td class="format-money" style="color: var(--success)">${formatMoney(totalThucNhan)}</td>
                            <td></td>
                        </tr>
                    </tfoot>
                </table>
            `;
        }
        resultBox.classList.remove('hidden');
    } catch (error) {
        resultBox.innerHTML = `<p style="color: var(--danger)">${error.message}</p>`;
        resultBox.classList.remove('hidden');
    }
});

document.getElementById('createEmpForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const resultBox = document.getElementById('createEmpResult');
    const payload = {
        id: document.getElementById('newEmpId').value,
        fullName: document.getElementById('newEmpName').value,
        phoneNumber: document.getElementById('newEmpPhone').value,
        bankName: document.getElementById('newEmpBankName').value,
        bankAccountNumber: document.getElementById('newEmpBankAcc').value,
        isActive: true
    };
    
    try {
        const res = await fetch(`${API_BASE}/employees`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('Không thể tạo nhân viên mới');
        const newId = await res.text();
        
        resultBox.innerHTML = `
            <div style="text-align: center;">
                <p style="color: var(--success); font-size: 1.1rem; font-weight: bold; margin-bottom: 0.5rem;">Tạo thành công!</p>
                <p>Mã nhân viên mới: <strong>#${newId}</strong> - Tên: <strong>${payload.fullName}</strong></p>
            </div>
        `;
        resultBox.classList.remove('hidden');
        
        // Clear input
        document.getElementById('newEmpId').value = '';
        document.getElementById('newEmpName').value = '';
        document.getElementById('newEmpPhone').value = '';
        
        // Cập nhật lại toàn bộ dữ liệu
        refreshAllData();
    } catch (error) {
        resultBox.innerHTML = `<p style="color: var(--danger)">${error.message}</p>`;
        resultBox.classList.remove('hidden');
    }
});

// ==========================================
// 2. BÁO CÁO LƯƠNG NHÂN VIÊN
// ==========================================
document.getElementById('payrollForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const empId = document.getElementById('empId').value;
    const month = document.getElementById('month').value;
    const year = document.getElementById('year').value;
    const resultBox = document.getElementById('payrollResult');
    
    try {
        const res = await fetch(`${API_BASE}/reports/monthly?employeeId=${empId}&month=${month}&year=${year}`);
        if (!res.ok) throw new Error('Không tìm thấy dữ liệu hoặc có lỗi xảy ra');
        const data = await res.json();
        
        let datesHtml = '';
        if (data.workDetails && data.workDetails.length > 0) {
            datesHtml = `
                <table class="dates-table">
                    <thead>
                        <tr>
                            <th>Ngày làm</th>
                            <th>Ca</th>
                            <th>Mức lương</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${data.workDetails.map(d => {
                            const shiftColors = {
                                'SÁNG': 'background: rgba(250, 204, 21, 0.2); color: #fde047;',
                                'CHIỀU': 'background: rgba(56, 189, 248, 0.2); color: #7dd3fc;',
                                'TỐI': 'background: rgba(168, 85, 247, 0.2); color: #d8b4fe;'
                            };
                            const shiftStyle = shiftColors[d.shift] || 'background: rgba(156, 163, 175, 0.2); color: #d1d5db;';
                            return `
                            <tr>
                                <td>${d.workDate}</td>
                                <td><span style="${shiftStyle} padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600;">${d.shift}</span></td>
                                <td class="format-money">${formatMoney(d.wage)}</td>
                            </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            `;
        } else {
            datesHtml = '<p style="color: var(--text-secondary); font-size: 0.875rem; margin-top: 1rem;">Không có ngày làm việc nào trong tháng này.</p>';
        }

        let advancesHtml = '';
        if (data.advanceDetails && data.advanceDetails.length > 0) {
            advancesHtml = `
                <h4 style="margin-top: 2rem; margin-bottom: 0.5rem; color: var(--text-secondary);">Lịch sử Thanh toán / Ứng lương:</h4>
                <table class="dates-table">
                    <thead>
                        <tr>
                            <th>Mã phiếu</th>
                            <th>Ngày thanh toán</th>
                            <th>Số tiền</th>
                            <th>Ghi chú</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${data.advanceDetails.map(a => `
                            <tr>
                                <td>#${a.id}</td>
                                <td>${a.advanceDate}</td>
                                <td class="format-money" style="color: var(--danger)">${formatMoney(a.amount)}</td>
                                <td style="font-size: 0.85rem">${a.notes || ''}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        }

        const isNegative = data.actualReceived < 0;

        let qrBtnHtml = '';
        if (data.actualReceived > 0 && currentUser && currentUser.roles.includes('ROLE_ADMIN')) {
            const bName = data.bankName ? `'${data.bankName}'` : 'null';
            const bAcc = data.bankAccountNumber ? `'${data.bankAccountNumber}'` : 'null';
            qrBtnHtml = `
                <button type="button" class="btn-primary" style="background: var(--primary); margin-top: 1rem;" 
                    onclick="showQrModal(${bName}, ${bAcc}, ${data.actualReceived}, 'Thanh toan luong ${empId}', () => { 
                        document.getElementById('payAmount').value = ${data.actualReceived};
                        document.getElementById('payNotes').value = 'Thanh toán lương tháng ${month}/${year}';
                        window.scrollTo({ top: document.getElementById('payForm').offsetTop, behavior: 'smooth' });
                    })">
                    <span class="icon">📱</span> Hiển Thị Mã QR Thanh Toán
                </button>
            `;
        }

        resultBox.innerHTML = `
            <div class="result-item"><span>Tổng Lương (Từ các ca):</span> <span class="format-money">${formatMoney(data.totalWage)}</span></div>
            <div class="result-item"><span>Đã Tạm Ứng:</span> <span class="format-money">${formatMoney(data.totalAdvance)}</span></div>
            <div class="result-item highlight ${isNegative ? 'danger' : ''}" style="display: flex; justify-content: space-between; align-items: center;">
                <span>Thực Nhận:</span> 
                <span class="format-money">${formatMoney(data.actualReceived)}</span>
            </div>
            ${qrBtnHtml}
            ${datesHtml}
            ${advancesHtml}
        `;
        resultBox.classList.remove('hidden');

        // Tự động điền ID nhân viên vào form thanh toán bên dưới
        document.getElementById('payEmpId').value = empId;
    } catch (error) {
        resultBox.innerHTML = `<p style="color: var(--danger)">${error.message}</p>`;
        resultBox.classList.remove('hidden');
    }
});

// Set ngày thanh toán mặc định là hôm nay
document.getElementById('payDate').value = new Date().toISOString().split('T')[0];

document.getElementById('payForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const resultBox = document.getElementById('payResult');
    
    const payload = {
        employeeId: document.getElementById('payEmpId').value,
        amount: parseFloat(document.getElementById('payAmount').value),
        advanceDate: document.getElementById('payDate').value,
        notes: document.getElementById('payNotes').value
    };

    try {
        const res = await fetch(`${API_BASE}/advance-payments`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (!res.ok) {
            const errText = await res.text();
            throw new Error(`Không thể tạo phiếu thanh toán. Backend báo lỗi: ${errText}`);
        }
        const newId = await res.json();
        
        resultBox.innerHTML = `
            <div style="text-align: center;">
                <p style="color: var(--success); font-size: 1.1rem; font-weight: bold; margin-bottom: 0.5rem;">Thanh toán thành công!</p>
                <p>Mã phiếu: <strong>#${newId}</strong>. Đã trừ vào Thực Nhận.</p>
            </div>
        `;
        resultBox.classList.remove('hidden');
        
        // Cập nhật lại toàn bộ dữ liệu
        refreshAllData();
        
        // Clear input
        document.getElementById('payAmount').value = '';
        document.getElementById('payNotes').value = '';
    } catch (error) {
        resultBox.innerHTML = `<p style="color: var(--danger)">${error.message}</p>`;
        resultBox.classList.remove('hidden');
    }
});


// ==========================================
// 3. TỔNG KẾT CA LÀM VIỆC
// ==========================================
document.getElementById('summaryForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const scheduleId = document.getElementById('scheduleId').value;
    const resultBox = document.getElementById('summaryResult');
    
    try {
        const res = await fetch(`${API_BASE}/work-schedules/${scheduleId}/summary`);
        if (!res.ok) throw new Error('Không tìm thấy ca làm việc');
        const data = await res.json();
        
        resultBox.innerHTML = `
            <div class="result-item"><span>Ngày làm:</span> <span>${data.workDate} (${data.shift})</span></div>
            <div class="result-item"><span>Địa điểm:</span> <span>${data.locationName}</span></div>
            <div class="result-item"><span>Trạng thái:</span> <span style="color: var(--success)">${data.paymentStatus}</span></div>
            <hr style="border: 0; border-top: 1px dashed var(--border-color); margin: 1rem 0;">
            <div class="result-item"><span>Doanh thu (Thành tiền):</span> <span class="format-money">${formatMoney(data.thanhTien)}</span></div>
            <div class="result-item"><span>Lương trả nhân viên:</span> <span class="format-money">${formatMoney(data.luongNhanVien)}</span></div>
            <div class="result-item highlight"><span>Tiền cắt (Lợi nhuận):</span> <span class="format-money">${formatMoney(data.tienCat)}</span></div>
        `;
        resultBox.classList.remove('hidden');
    } catch (error) {
        resultBox.innerHTML = `<p style="color: var(--danger)">${error.message}</p>`;
        resultBox.classList.remove('hidden');
    }
});


// ==========================================
// 4. TẠO CA MỚI VÀ TÍNH LƯƠNG TRỰC TIẾP
// ==========================================
function calculateLiveWage() {
    let total = 0;
    document.querySelectorAll('.emp-wage-input').forEach(input => {
        if (input.value) total += parseFloat(input.value);
    });
    document.getElementById('liveTotalWage').innerHTML = `Tổng lương NV trong đám: <span class="format-money">${formatMoney(total)}</span>`;
}

// Lắng nghe sự kiện sửa lương ở danh sách mặc định
document.getElementById('employeeList').addEventListener('input', (e) => {
    if (e.target.classList.contains('emp-wage-input')) {
        calculateLiveWage();
    }
});
// Tính toán lần đầu
calculateLiveWage();

function generateEmployeeRows() {
    const quantity = parseInt(document.getElementById('quantity').value) || 0;
    const casualCount = parseInt(document.getElementById('casualWorkerCount').value) || 0;
    const targetCount = Math.max(0, quantity - casualCount);
    
    const empList = document.getElementById('employeeList');
    const currentRows = empList.querySelectorAll('.employee-row');
    
    if (currentRows.length < targetCount) {
        // Thêm các dòng còn thiếu
        for (let i = currentRows.length; i < targetCount; i++) {
            const newRow = document.createElement('div');
            newRow.className = 'employee-row';
            newRow.innerHTML = `
                <input type="text" class="emp-id-input" placeholder="ID Nhân viên" list="employeeSuggestions" required>
                <input type="number" class="emp-wage-input" placeholder="Mức lương" required>
            `;
            empList.appendChild(newRow);
        }
    } else if (currentRows.length > targetCount) {
        // Xóa bớt các dòng dư thừa ở cuối
        for (let i = currentRows.length - 1; i >= targetCount; i--) {
            currentRows[i].remove();
        }
    }
    calculateLiveWage();
}

document.getElementById('quantity').addEventListener('input', generateEmployeeRows);
document.getElementById('casualWorkerCount').addEventListener('input', generateEmployeeRows);

// Chạy lần đầu khi load trang
generateEmployeeRows();

// ==========================================
// 5. HIỂN THỊ CHI TIẾT CA LÀM VIỆC (MODAL)
// ==========================================
window.showScheduleDetails = function(id) {
    const s = currentSchedules[id];
    if(!s) return;
    
    document.getElementById('modalScheduleId').innerText = `#${s.id}`;
    
    let empsHtml = '';
    if (s.employees && s.employees.length > 0) {
        empsHtml = `
            <table class="dates-table" style="margin-top: 1rem;">
                <thead>
                    <tr><th>ID NV</th><th>Họ tên</th><th>Mức lương</th></tr>
                </thead>
                <tbody>
                    ${s.employees.map(e => `
                        <tr>
                            <td>${e.employeeId}</td>
                            <td>${e.fullName}</td>
                            <td class="format-money">${formatMoney(e.wage)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    } else {
        empsHtml = '<p style="color: var(--text-secondary); margin-top: 1rem; font-style: italic;">Không có nhân viên cố định nào làm ca này.</p>';
    }

    document.getElementById('modalContent').innerHTML = `
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; margin-bottom: 1.5rem;">
            <div><span style="color: var(--text-secondary)">Ngày làm:</span> <br><strong>${s.workDate}</strong></div>
            <div><span style="color: var(--text-secondary)">Ca làm:</span> <br><span style="background: rgba(45,212,191,0.2); padding: 2px 8px; border-radius: 12px; font-size: 0.85rem; font-weight: bold;">${s.shift}</span></div>
            
            <div style="background: rgba(168,85,247,0.1); padding: 1rem; border-radius: 0.5rem; border: 1px solid rgba(168,85,247,0.3);">
                <span style="color: var(--text-secondary); font-size: 0.9rem;">Tổng doanh thu (Tiền đám):</span> <br>
                <span class="format-money" style="color: #a855f7; font-weight:bold; font-size: 1.25rem;">${formatMoney(s.thanhTien)}</span>
            </div>
            
            <div style="background: rgba(16,185,129,0.1); padding: 1rem; border-radius: 0.5rem; border: 1px solid rgba(16,185,129,0.3);">
                <span style="color: var(--text-secondary); font-size: 0.9rem;">Tổng tiền cắt (Lợi nhuận):</span> <br>
                <span class="format-money" style="color: var(--success); font-weight:bold; font-size: 1.25rem;">${formatMoney(s.tienCat)}</span>
            </div>
            
            <div><span style="color: var(--text-secondary)">Lương NV thuê ngoài:</span> <br><span class="format-money" style="color: #f43f5e; font-weight:bold;">${formatMoney(s.casualWage)}</span></div>
            <div><span style="color: var(--text-secondary)">Lương NV cố định:</span> <br><span class="format-money" style="color: #6366f1; font-weight:bold;">${formatMoney(s.luongNhanVien)}</span></div>
            
            <div><span style="color: var(--text-secondary)">Tiền ăn:</span> <br><span class="format-money">${formatMoney(s.mealAllowance)}</span></div>
            <div><span style="color: var(--text-secondary)">Trạng thái:</span> <br><span style="color: ${s.paymentStatus === 'ĐÃ NHẬN' ? 'var(--success)' : 'var(--danger)'}; font-weight: bold;">${s.paymentStatus}</span></div>
        </div>
        
        <h3 style="color: var(--text-primary); border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 0.5rem; margin-top: 1rem;">Danh Sách Nhân Viên Đi Làm</h3>
        ${empsHtml}
    `;
    
    document.getElementById('scheduleModal').classList.remove('hidden');
};

window.deleteWorkSchedule = async function(id) {
    if(!confirm('Bạn có chắc chắn muốn xóa ca làm việc #' + id + ' này không?\nThao tác này sẽ xóa mọi dữ liệu và tiền lương liên quan đến ca này!')) {
        return;
    }
    
    try {
        const res = await fetch(`${API_BASE}/work-schedules/${id}`, {
            method: 'DELETE'
        });
        
        if (!res.ok) throw new Error('Không thể xóa ca làm việc');
        
        alert('Đã xóa thành công ca làm việc #' + id);
        
        // Tải lại toàn bộ dữ liệu
        refreshAllData();
    } catch (e) {
        alert('Lỗi: ' + e.message);
    }
};

window.editWorkSchedule = function(id) {
    const s = currentSchedules[id];
    if(!s) return;
    
    editingScheduleId = id;
    
    // Switch to create tab
    document.querySelector('.nav-item[data-target="create-page"]').click();
    
    // Change form button text and title
    document.querySelector('#createScheduleForm button[type="submit"]').innerText = 'Cập Nhật Ca';
    document.querySelector('#create-page h2').innerText = 'Sửa Ca Làm Việc #' + id;
    
    // Populate scalar fields
    document.getElementById('workDate').value = s.workDate;
    document.getElementById('shift').value = s.shift;
    
    // Find location ID
    const locSelect = document.getElementById('locationId');
    for (let i = 0; i < locSelect.options.length; i++) {
        if (locSelect.options[i].text === s.locationName) {
            locSelect.selectedIndex = i;
            break;
        }
    }
    
    document.getElementById('unitPrice').value = s.unitPrice;
    document.getElementById('quantity').value = s.quantity;
    document.getElementById('casualWage').value = s.casualWage;
    document.getElementById('paymentStatus').value = s.paymentStatus;
    
    document.getElementById('casualWorkerCount').value = s.casualWorkerCount || 0;
    
    generateEmployeeRows();
    
    // Populate employees
    const rows = document.querySelectorAll('.employee-row');
    if (s.employees) {
        for(let i=0; i<s.employees.length; i++) {
            if (i < rows.length) {
                rows[i].querySelector('.emp-id-input').value = s.employees[i].employeeId;
                rows[i].querySelector('.emp-wage-input').value = s.employees[i].wage;
            }
        }
    }
    calculateLiveWage();
};

document.getElementById('createScheduleForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const resultBox = document.getElementById('createResult');
    
    // Thu thập danh sách nhân viên
    const employees = [];
    document.querySelectorAll('.employee-row').forEach(row => {
        const id = row.querySelector('.emp-id-input').value;
        const wage = row.querySelector('.emp-wage-input').value;
        if (id && wage) {
            employees.push({ employeeId: id, wage: parseFloat(wage) });
        }
    });

    const payload = {
        workDate: document.getElementById('workDate').value,
        shift: document.getElementById('shift').value,
        locationId: parseInt(document.getElementById('locationId').value),
        unitPrice: parseFloat(document.getElementById('unitPrice').value),
        quantity: parseInt(document.getElementById('quantity').value),
        mealAllowance: 0,
        casualWage: parseFloat(document.getElementById('casualWage')?.value || 0),
        paymentStatus: document.getElementById('paymentStatus').value,
        employees: employees
    };

    try {
        let res;
        if (editingScheduleId) {
            res = await fetch(`${API_BASE}/work-schedules/${editingScheduleId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error('Lỗi khi cập nhật ca làm việc');
            
            resultBox.innerHTML = `
                <div style="text-align: center;">
                    <p style="color: var(--success); font-size: 1.2rem; font-weight: bold; margin-bottom: 0.5rem;">Cập nhật thành công!</p>
                    <p>Đã cập nhật ca làm việc ID: <strong>${editingScheduleId}</strong></p>
                </div>
            `;
            
            // Reset state
            editingScheduleId = null;
            document.querySelector('#createScheduleForm button[type="submit"]').innerText = 'Tạo Ca Mới';
            document.querySelector('#create-page h2').innerText = 'Tạo Ca Mới';
        } else {
            res = await fetch(`${API_BASE}/work-schedules`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error('Lỗi khi tạo ca làm việc');
            const newId = await res.json();
            
            resultBox.innerHTML = `
                <div style="text-align: center;">
                    <p style="color: var(--success); font-size: 1.2rem; font-weight: bold; margin-bottom: 0.5rem;">Thành công!</p>
                    <p>Đã tạo ca làm việc mới với ID: <strong>${newId}</strong></p>
                    <p style="font-size: 0.875rem; color: var(--text-secondary); margin-top: 0.5rem;">Hãy chuyển sang tab "Tổng Kết Ca" để kiểm tra.</p>
                </div>
            `;
            
            // Xóa form sau khi tạo thành công
            document.getElementById('createScheduleForm').reset();
            generateEmployeeRows();
        }
        
        // Cập nhật lại toàn bộ dữ liệu để load ca mới hoặc ca vừa sửa
        refreshAllData();
        
        resultBox.classList.remove('hidden');
    } catch (error) {
        resultBox.innerHTML = `<p style="color: var(--danger)">${error.message}</p>`;
        resultBox.classList.remove('hidden');
    }
});

// Tự động tải dữ liệu sau khi tất cả sự kiện đã được gán
function autoLoadData() {
    setTimeout(() => {
        const overviewForm = document.getElementById('overviewForm');
        if (overviewForm) overviewForm.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
        
        const empForm = document.getElementById('employeesForm');
        if (empForm) empForm.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
    }, 100);
}

let currentUser = null;

async function checkAuthAndLoadData() {
    try {
        const authRes = await fetch(`${API_BASE}/auth/me`);
        if (authRes.ok) {
            currentUser = await authRes.json();
            applyRoleBasedUI(currentUser);
        }
    } catch (e) {
        console.error("Lỗi xác thực:", e);
    }
    refreshAllData();
}

// ==========================================
// ADVANCE REQUESTS LOGIC
// ==========================================
async function loadAdvanceRequests() {
    try {
        const endpoint = currentUser && currentUser.roles.includes('ROLE_ADMIN') ? '' : '/me';
        const res = await fetch(`${API_BASE}/advance-requests${endpoint}`);
        if (!res.ok) throw new Error('Không thể tải yêu cầu ứng lương');
        const requests = await res.json();
        
        const tbody = document.querySelector('#advanceRequestsTable tbody');
        if (requests.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Chưa có yêu cầu ứng lương nào.</td></tr>';
            return;
        }

        tbody.innerHTML = requests.map(req => {
            const dateStr = new Date(req.requestDate).toLocaleString('vi-VN');
            const empName = req.employee ? `${req.employee.fullName} (${req.employee.id})` : '';
            
            let statusHtml = '';
            if (req.status === 'PENDING') statusHtml = '<span style="color: #f59e0b; font-weight: bold;">Đang xử lý</span>';
            else if (req.status === 'PAID') statusHtml = '<span style="color: var(--success); font-weight: bold;">Đã thanh toán</span>';
            else if (req.status === 'REJECTED') statusHtml = '<span style="color: var(--danger); font-weight: bold;">Đã từ chối</span>';
                
            let actionHtml = '-';
            if (req.status === 'PENDING' && currentUser && currentUser.roles.includes('ROLE_ADMIN')) {
                const bName = req.employee && req.employee.bankName ? `'${req.employee.bankName}'` : 'null';
                const bAcc = req.employee && req.employee.bankAccountNumber ? `'${req.employee.bankAccountNumber}'` : 'null';
                actionHtml = `
                    <div style="display: flex; gap: 0.5rem; justify-content: center;">
                        <button class="btn-primary" style="padding: 0.25rem 0.75rem; font-size: 0.8rem; background: var(--success);" onclick="payAdvanceRequest(${req.id}, ${bName}, ${bAcc}, ${req.amount}, '${empName}')">Thanh toán</button>
                        <button class="btn-primary" style="padding: 0.25rem 0.75rem; font-size: 0.8rem; background: var(--danger);" onclick="rejectAdvanceRequest(${req.id})">Từ chối</button>
                    </div>
                `;
            }

            return `
                <tr>
                    <td>${dateStr}</td>
                    <td>${empName}</td>
                    <td class="format-money">${formatMoney(req.amount)}</td>
                    <td>${req.reason}</td>
                    <td>${statusHtml}</td>
                    <td>${actionHtml}</td>
                </tr>
            `;
        }).join('');
    } catch (e) {
        console.error(e);
    }
}

document.getElementById('advanceRequestForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const amount = document.getElementById('requestAmount').value;
    const reason = document.getElementById('requestReason').value;
    
    try {
        const res = await fetch(`${API_BASE}/advance-requests`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount: parseFloat(amount), reason })
        });
        if (!res.ok) throw new Error(await res.text());
        alert('Đã gửi yêu cầu ứng lương thành công!');
        document.getElementById('advanceRequestForm').reset();
        loadAdvanceRequests();
    } catch (err) {
        alert('Lỗi: ' + err.message);
    }
});

window.payAdvanceRequest = async function(id, bankId, accountNo, amount, empName) {
    if (!bankId || !accountNo) {
        if(!confirm(`Xác nhận đã thanh toán bằng TIỀN MẶT số tiền ${formatMoney(amount)} cho ${empName}?`)) return;
        executePay(id);
    } else {
        const content = `Tam ung luong ${empName}`;
        showQrModal(bankId, accountNo, amount, content, () => {
            executePay(id);
        });
    }
};

async function executePay(id) {
    try {
        const res = await fetch(`${API_BASE}/advance-requests/${id}/pay`, { method: 'PUT' });
        if (!res.ok) throw new Error(await res.text());
        alert('Đã cập nhật trạng thái thanh toán thành công!');
        loadAdvanceRequests();
    } catch (err) {
        alert('Lỗi: ' + err.message);
    }
}

window.rejectAdvanceRequest = async function(id) {
    const reason = prompt('Nhập lý do từ chối (Không bắt buộc):');
    if(reason === null) return; // User cancelled
    
    try {
        const res = await fetch(`${API_BASE}/advance-requests/${id}/reject`, { method: 'PUT' });
        if (!res.ok) throw new Error(await res.text());
        alert('Đã từ chối yêu cầu ứng lương này!');
        loadAdvanceRequests();
    } catch (err) {
        alert('Lỗi: ' + err.message);
    }
};

function applyRoleBasedUI(user) {
    if (!user || !user.roles) return;
    
    const isAdmin = user.roles.includes('ROLE_ADMIN');
    
    if (!isAdmin) {
        // ... (admin tabs hiding)
        if(document.getElementById('menu-overview')) document.getElementById('menu-overview').style.display = 'none';
        if(document.getElementById('menu-employees')) document.getElementById('menu-employees').style.display = 'none';
        if(document.getElementById('menu-schedules')) document.getElementById('menu-schedules').style.display = 'none';
        if(document.getElementById('menu-create')) document.getElementById('menu-create').style.display = 'none';
        
        // Hide Payment Section
        const paySection = document.getElementById('pay-section');
        if (paySection) paySection.style.display = 'none';
        
        // Auto-fill empId in Salary Report and disable it
        const empIdInput = document.getElementById('empId');
        if (empIdInput) {
            empIdInput.value = user.username;
            empIdInput.readOnly = true;
            empIdInput.style.background = 'rgba(0,0,0,0.3)';
            empIdInput.style.cursor = 'not-allowed';
        }
        
        // Switch to Salary Report tab by default since others are hidden
        if(document.getElementById('menu-payroll')) document.getElementById('menu-payroll').click();
        
        // Show advance request form for users
        if(document.getElementById('advanceRequestFormContainer')) document.getElementById('advanceRequestFormContainer').style.display = 'block';
    }
    
    // Profile Info
    if (user.bankName) document.getElementById('myBankName').value = user.bankName;
    if (user.bankAccountNumber) document.getElementById('myBankAcc').value = user.bankAccountNumber;
    
    document.getElementById('profileForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const bankName = document.getElementById('myBankName').value;
        const bankAccountNumber = document.getElementById('myBankAcc').value;
        try {
            const res = await fetch(`${API_BASE}/employees/me/bank`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ bankName, bankAccountNumber })
            });
            if (!res.ok) throw new Error('Không thể cập nhật ngân hàng');
            alert('Cập nhật tài khoản ngân hàng thành công!');
        } catch (err) {
            alert('Lỗi: ' + err.message);
        }
    });

    // Check if First Login
    if (user.isFirstLogin === true) {
        document.getElementById('changePasswordModal').style.display = 'flex';
        
        document.getElementById('btnChangePassword').addEventListener('click', async () => {
            const newPwd = document.getElementById('newPasswordInput').value;
            if (!newPwd || newPwd.trim().length < 4) {
                alert('Vui lòng nhập mật khẩu hợp lệ (ít nhất 4 ký tự)');
                return;
            }
            try {
                const res = await fetch(`${API_BASE}/auth/change-password`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ newPassword: newPwd })
                });
                if (!res.ok) throw new Error('Không thể đổi mật khẩu');
                
                alert('Đổi mật khẩu thành công! Hệ thống đã ghi nhận.');
                document.getElementById('changePasswordModal').style.display = 'none';
            } catch (err) {
                alert('Lỗi: ' + err.message);
            }
        });
    }
}

// ==========================================
// QR CODE LOGIC
// ==========================================
window.showQrModal = function(bankId, accountNo, amount, content, onConfirm) {
    const modal = document.getElementById('qrModal');
    const contentDiv = document.getElementById('qrContent');
    const confirmBtn = document.getElementById('confirmQrPayBtn');
    
    if (!bankId || !accountNo) {
        contentDiv.innerHTML = `
            <div style="padding: 2rem 0; color: var(--danger);">
                <span style="font-size: 3rem;">⚠️</span>
                <p style="margin-top: 1rem;">Nhân viên này chưa được cập nhật thông tin Ngân hàng!</p>
            </div>
        `;
        confirmBtn.style.display = 'none';
    } else {
        const qrUrl = `https://img.vietqr.io/image/${bankId}-${accountNo}-compact2.png?amount=${amount}&addInfo=${encodeURIComponent(content)}`;
        contentDiv.innerHTML = `
            <img src="${qrUrl}" style="width: 100%; max-width: 300px; border-radius: 1rem; border: 2px solid rgba(255,255,255,0.1);">
            <div style="background: rgba(255,255,255,0.05); padding: 1rem; border-radius: 0.5rem; width: 100%; text-align: left;">
                <p style="margin: 0.25rem 0; color: var(--text-secondary); font-size: 0.9rem;">Ngân hàng: <strong style="color: white;">${bankId.toUpperCase()}</strong></p>
                <p style="margin: 0.25rem 0; color: var(--text-secondary); font-size: 0.9rem;">Số TK: <strong style="color: white;">${accountNo}</strong></p>
                <p style="margin: 0.25rem 0; color: var(--text-secondary); font-size: 0.9rem;">Số tiền: <strong style="color: var(--success);">${formatMoney(amount)}</strong></p>
            </div>
        `;
        confirmBtn.style.display = 'block';
        confirmBtn.onclick = () => {
            closeQrModal();
            if(onConfirm) onConfirm();
        };
    }
    modal.classList.remove('hidden');
};

window.closeQrModal = function() {
    document.getElementById('qrModal').classList.add('hidden');
};

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', checkAuthAndLoadData);
} else {
    checkAuthAndLoadData();
}
