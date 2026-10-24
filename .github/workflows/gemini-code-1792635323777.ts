"use client";

import React, { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { 
  Shield, 
  Award, 
  BookOpen, 
  Calendar, 
  Users, 
  QrCode, 
  X, 
  Menu, 
  ChevronRight, 
  Phone, 
  Mail, 
  MapPin, 
  ExternalLink,
  Flame,
  Search,
  Star
} from "lucide-react";

export default function TSQPB70Years() {
  const [activeTab, setActiveTab] = useState<string>("home");
  const [showQR, setShowQR] = useState<boolean>(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);

  // Tên miền sự kiện chính thức
  const websiteUrl = "https://70nam_tsqpb.edu.vn";

  // Dữ liệu dòng thời gian 70 năm
  const milestones = [
    {
      year: "18/02/1957",
      title: "Thành lập Trường Sĩ quan Pháo binh",
      desc: "Thành lập theo Nghị định số 32 NĐ-A của Bộ trưởng Bộ Quốc phòng kiêm Tổng Tư lệnh Quân đội nhân dân Việt Nam, đặt nền móng cho hệ thống đào tạo sĩ quan chỉ huy pháo binh chính quy."
    },
    {
      year: "1957 - 1975",
      title: "Chiến đấu & Chi viện hỏa lực cho chiến trường miền Nam",
      desc: "Vừa đào tạo vừa tham gia chiến đấu, đào tạo hàng vạn cán bộ chỉ huy pháo binh tinh nhuệ cho các chiến dịch lịch sử, lập nên truyền thống 'Chân đồng vai sắt, đánh giỏi, bắn trúng'."
    },
    {
      year: "Thời kỳ Đổi mới",
      title: "Chuẩn hóa và Hiện đại hóa Giáo dục - Đào tạo",
      desc: "Nâng cấp cơ sở vật chất, doanh trại chính quy tại xã Thanh Mỹ, thị xã Sơn Tây, TP Hà Nội; đẩy mạnh nghiên cứu khoa học quân sự và ứng dụng công nghệ mô phỏng tác chiến."
    },
    {
      year: "Giai đoạn 2021 - 2027",
      title: "Chuyển đổi số & Tự hào chặng đường 70 năm Anh hùng",
      desc: "Đẩy mạnh chuyển đổi số giảng dạy, kiểm định chất lượng đào tạo chuẩn quốc gia, vững bước trở thành trung tâm đào tạo sĩ quan pháo binh hàng đầu toàn quân."
    }
  ];

  // Danh mục phần thưởng cao quý
  const honors = [
    { title: "Anh hùng Lực lượng Vũ trang Nhân dân", desc: "Danh hiệu cao quý do Đảng và Nhà nước trao tặng vì chiến công đặc biệt xuất sắc." },
    { title: "Huân chương Sao vàng", desc: "Phần thưởng danh giá nhất của Nhà nước ghi nhận đóng góp vĩ đại trong sự nghiệp cách mạng." },
    { title: "Huân chương Hồ Chí Minh", desc: "Ghi nhận công lao vẻ vang trong sự nghiệp đào tạo cán bộ sĩ quan pháo binh cho toàn quân." },
    { title: "Huân chương Quân công & Chiến công", desc: "Nhiều tập thể, khoa giáo viên và học viên được tặng thưởng qua các thời kỳ." }
  ];

  // Tin tức nổi bật theo phong cách cổng tsqpb.edu.vn
  const newsList = [
    { title: "Hướng tới Đại lễ Kỷ niệm 70 năm Ngày truyền thống Trường Sĩ quan Pháo binh (18/02/1957 - 18/02/2027)", date: "2026-10-22", tag: "Sự kiện" },
    { title: "Đẩy mạnh phong trào thi đua dạy tốt, học tốt, rèn luyện nghiêm chào mừng 70 năm thành lập", date: "2026-10-18", tag: "Thi đua" },
    { title: "Công tác chuẩn bị hội thảo khoa học: Bề dày lịch sử và nghệ thuật tác chiến Pháo binh Việt Nam", date: "2026-10-15", tag: "Nghiên cứu" },
    { title: "Triển khai chương trình tự đánh giá chất lượng đào tạo sĩ quan chỉ huy phân đội trình độ đại học", date: "2026-10-10", tag: "Đào tạo" }
  ];

  return (
    <div className="min-h-screen bg-slate-100 text-slate-800 flex flex-col font-sans">
      {/* 1. TOPBAR CỔNG THÔNG TIN */}
      <div className="bg-red-950 text-slate-300 text-xs py-1.5 px-4 border-b border-red-900 hidden sm:block">
        <div className="max-w-7xl mx-auto flex justify-between items-center">
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1"><MapPin className="w-3.5 h-3.5 text-yellow-400" /> Thanh Mỹ, Thị xã Sơn Tây, TP. Hà Nội</span>
            <span className="flex items-center gap-1"><Phone className="w-3.5 h-3.5 text-yellow-400" /> (+84-24) 33838194</span>
            <span className="flex items-center gap-1"><Mail className="w-3.5 h-3.5 text-yellow-400" /> tuyensinhsqpb@gmail.com</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-yellow-400 font-semibold">Tên miền chính thức:</span>
            <span className="bg-red-900 px-2 py-0.5 rounded text-white font-mono">{websiteUrl}</span>
          </div>
        </div>
      </div>

      {/* 2. BANNER HEADER TRUYỀN THỐNG CHUẨN QUÂN ĐỘI */}
      <header className="bg-gradient-to-r from-red-900 via-red-800 to-red-950 text-white shadow-md border-b-4 border-yellow-500">
        <div className="max-w-7xl mx-auto px-4 py-4 sm:py-6 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-4 text-center md:text-left">
            {/* Logo Pháo binh */}
            <div className="w-16 h-16 sm:w-20 sm:h-20 bg-white rounded-full p-1 border-2 border-yellow-400 shadow-md flex items-center justify-center flex-shrink-0">
              <Shield className="w-12 h-12 text-red-800" />
            </div>
            <div>
              <p className="text-xs sm:text-sm font-bold uppercase tracking-widest text-yellow-300">
                BỘ QUỐC PHÒNG • BINH CHỦNG PHÁO BINH
              </p>
              <h1 className="text-xl sm:text-2xl md:text-3xl font-black uppercase tracking-tight text-white drop-shadow">
                TRƯỜNG SĨ QUAN PHÁO BINH
              </h1>
              <p className="text-xs sm:text-sm text-yellow-200 font-medium">
                CỔNG THÔNG TIN ĐIỆN TỬ KỶ NIỆM 70 NĂM NGÀY TRUYỀN THỐNG (18/02/1957 - 18/02/2027)
              </p>
            </div>
          </div>

          {/* Huy hiệu 70 năm & nút QR */}
          <div className="flex items-center gap-3">
            <div className="text-center px-3 py-1.5 bg-yellow-500/20 rounded-lg border border-yellow-400/50">
              <span className="block text-xl font-extrabold text-yellow-400 leading-none">70 NĂM</span>
              <span className="text-[10px] uppercase font-bold text-slate-200">Xây dựng & Trưởng thành</span>
            </div>
            <button
              onClick={() => setShowQR(true)}
              className="flex items-center gap-1.5 bg-yellow-500 hover:bg-yellow-400 text-red-950 font-bold px-3 py-2 rounded shadow text-xs sm:text-sm transition"
            >
              <QrCode className="w-4 h-4" />
              Mã QR
            </button>
          </div>
        </div>

        {/* 3. THANH ĐIỀU HƯỚNG CHÍNH */}
        <nav className="bg-red-950/80 backdrop-blur border-t border-red-800">
          <div className="max-w-7xl mx-auto px-4 flex justify-between items-center">
            <div className="hidden md:flex space-x-1 text-sm font-semibold text-white">
              <button 
                onClick={() => setActiveTab("home")} 
                className={`px-4 py-3 hover:bg-red-900 transition ${activeTab === "home" ? "bg-red-800 text-yellow-300 border-b-2 border-yellow-400" : ""}`}
              >
                Trang chủ
              </button>
              <button 
                onClick={() => setActiveTab("milestones")} 
                className={`px-4 py-3 hover:bg-red-900 transition ${activeTab === "milestones" ? "bg-red-800 text-yellow-300 border-b-2 border-yellow-400" : ""}`}
              >
                70 năm Lịch sử (1957 - 2027)
              </button>
              <button 
                onClick={() => setActiveTab("tradition")} 
                className={`px-4 py-3 hover:bg-red-900 transition ${activeTab === "tradition" ? "bg-red-800 text-yellow-300 border-b-2 border-yellow-400" : ""}`}
              >
                Truyền thống vẻ vang
              </button>
              <button 
                onClick={() => setActiveTab("honors")} 
                className={`px-4 py-3 hover:bg-red-900 transition ${activeTab === "honors" ? "bg-red-800 text-yellow-300 border-b-2 border-yellow-400" : ""}`}
              >
                Thành tích & Khen thưởng
              </button>
              <button 
                onClick={() => setActiveTab("figures")} 
                className={`px-4 py-3 hover:bg-red-900 transition ${activeTab === "figures" ? "bg-red-800 text-yellow-300 border-b-2 border-yellow-400" : ""}`}
              >
                Nhân vật tiêu biểu
              </button>
            </div>

            {/* Mobile menu button */}
            <div className="md:hidden py-2 flex justify-between w-full">
              <span className="text-xs uppercase font-bold text-yellow-400 self-center">Chuyên mục</span>
              <button onClick={() => setMobileMenuOpen(!mobileMenuOpen)} className="p-1 text-white">
                {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
              </button>
            </div>
          </div>

          {/* Dropdown Mobile */}
          {mobileMenuOpen && (
            <div className="md:hidden bg-red-950 px-4 py-2 space-y-1 text-sm border-t border-red-800">
              <button onClick={() => { setActiveTab("home"); setMobileMenuOpen(false); }} className="block w-full text-left py-2 text-white">Trang chủ</button>
              <button onClick={() => { setActiveTab("milestones"); setMobileMenuOpen(false); }} className="block w-full text-left py-2 text-white">70 năm Lịch sử</button>
              <button onClick={() => { setActiveTab("tradition"); setMobileMenuOpen(false); }} className="block w-full text-left py-2 text-white">Truyền thống vẻ vang</button>
              <button onClick={() => { setActiveTab("honors"); setMobileMenuOpen(false); }} className="block w-full text-left py-2 text-white">Thành tích & Khen thưởng</button>
              <button onClick={() => { setActiveTab("figures"); setMobileMenuOpen(false); }} className="block w-full text-left py-2 text-white">Nhân vật tiêu biểu</button>
            </div>
          )}
        </nav>
      </header>

      {/* 4. KHẨU HIỆU TRUYỀN THỐNG CHẠY NỔI BẬT */}
      <div className="bg-yellow-400 text-red-950 font-bold py-2 px-4 shadow text-center text-xs sm:text-sm uppercase tracking-wider flex items-center justify-center gap-2">
        <Flame className="w-4 h-4 fill-red-800 text-red-800" />
        Truyền thống Bộ đội Pháo binh: "Chân đồng vai sắt, đánh giỏi, bắn trúng"
        <Flame className="w-4 h-4 fill-red-800 text-red-800" />
      </div>

      {/* 5. NỘI DUNG CHÍNH (LAYOUT DẠNG CỔNG THÔNG TIN) */}
      <main className="max-w-7xl mx-auto px-4 py-8 flex-1 w-full grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* CỘT NỘI DUNG CHÍNH (2 CỘT TRÁI) */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* TAB TRANG CHỦ */}
          {activeTab === "home" && (
            <>
              {/* Khối giới thiệu 70 năm */}
              <div className="bg-white p-6 rounded-lg shadow-sm border border-slate-200">
                <div className="flex items-center gap-2 border-b-2 border-red-800 pb-2 mb-4">
                  <Star className="w-5 h-5 text-red-800 fill-red-800" />
                  <h2 className="text-lg sm:text-xl font-bold uppercase text-red-900">
                    Chào mừng Kỷ niệm 70 năm Thành lập Trường (1957 - 2027)
                  </h2>
                </div>
                <p className="text-slate-700 text-sm leading-relaxed mb-4">
                  Trường Sĩ quan Pháo binh được thành lập ngày <strong>18 tháng 2 năm 1957</strong> theo Nghị định số 32 NĐ-A của Bộ trưởng Bộ Quốc phòng kiêm Tổng Tư lệnh QĐND Việt Nam. Trải qua 70 năm xây dựng, chiến đấu và trưởng thành, Nhà trường là trung tâm đào tạo sĩ quan chỉ huy chiến thuật cấp phân đội chuyên ngành pháo binh trực thuộc Binh chủng Pháo binh.
                </p>
                <div className="p-4 bg-amber-50 rounded border-l-4 border-amber-500 text-xs sm:text-sm text-slate-700 italic">
                  "Phát huy truyền thống Đơn vị Anh hùng Lực lượng vũ trang nhân dân, cán bộ, giảng viên, học viên Nhà trường quyết tâm hoàn thành xuất sắc mọi nhiệm vụ giáo dục - đào tạo và sẵn sàng chiến đấu."
                </div>
              </div>

              {/* Tin bài mới */}
              <div className="bg-white p-6 rounded-lg shadow-sm border border-slate-200">
                <div className="flex items-center justify-between border-b-2 border-red-800 pb-2 mb-4">
                  <h3 className="text-base sm:text-lg font-bold uppercase text-red-900 flex items-center gap-2">
                    <BookOpen className="w-5 h-5 text-yellow-600" />
                    Tin tức hoạt động & Kỷ niệm
                  </h3>
                  <span className="text-xs text-red-700 font-semibold cursor-pointer hover:underline">Xem tất cả</span>
                </div>
                <div className="divide-y divide-slate-100">
                  {newsList.map((news, i) => (
                    <div key={i} className="py-3 flex flex-col sm:flex-row justify-between sm:items-center gap-1 group cursor-pointer">
                      <div className="flex items-start gap-2">
                        <ChevronRight className="w-4 h-4 text-red-800 mt-0.5 group-hover:translate-x-1 transition" />
                        <span className="text-sm font-medium text-slate-800 group-hover:text-red-800 transition">
                          {news.title}
                        </span>
                      </div>
                      <span className="text-xs text-slate-400 self-end sm:self-center font-mono whitespace-nowrap">{news.date}</span>
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}

          {/* TAB DẤU MỐC 70 NĂM LỊCH SỬ */}
          {activeTab === "milestones" && (
            <div className="bg-white p-6 rounded-lg shadow-sm border border-slate-200">
              <div className="border-b-2 border-red-800 pb-2 mb-6">
                <h2 className="text-xl font-bold uppercase text-red-900 flex items-center gap-2">
                  <Calendar className="w-6 h-6 text-yellow-600" />
                  Dấu mốc 70 năm Xây dựng và Trưởng thành (1957 - 2027)
                </h2>
              </div>
              <div className="relative border-l-2 border-red-800 ml-4 pl-6 space-y-6">
                {milestones.map((m, idx) => (
                  <div key={idx} className="relative">
                    <div className="absolute -left-[31px] top-1 w-4 h-4 rounded-full bg-yellow-500 border-2 border-white ring-2 ring-red-800" />
                    <span className="text-xs font-bold uppercase tracking-wider text-red-800 bg-red-100 px-2 py-0.5 rounded">
                      {m.year}
                    </span>
                    <h4 className="text-base font-bold text-slate-800 mt-1">{m.title}</h4>
                    <p className="text-slate-600 text-sm mt-1 leading-relaxed">{m.desc}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB THÀNH TÍCH & KHEN THƯỞNG */}
          {activeTab === "honors" && (
            <div className="bg-white p-6 rounded-lg shadow-sm border border-slate-200">
              <div className="border-b-2 border-red-800 pb-2 mb-6">
                <h2 className="text-xl font-bold uppercase text-red-900 flex items-center gap-2">
                  <Award className="w-6 h-6 text-yellow-600" />
                  Phần thưởng Cao quý của Đảng & Nhà nước
                </h2>
              </div>
              <div className="grid sm:grid-cols-2 gap-4">
                {honors.map((h, idx) => (
                  <div key={idx} className="p-4 bg-slate-50 border-l-4 border-yellow-500 rounded shadow-sm">
                    <h4 className="font-bold text-red-900 text-sm">{h.title}</h4>
                    <p className="text-slate-600 text-xs mt-1.5 leading-relaxed">{h.desc}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB TRUYỀN THỐNG VẺ VANG */}
          {activeTab === "tradition" && (
            <div className="bg-white p-6 rounded-lg shadow-sm border border-slate-200 space-y-4 text-sm leading-relaxed text-slate-700">
              <h2 className="text-xl font-bold uppercase text-red-900 border-b-2 border-red-800 pb-2">
                Nét son truyền thống 70 năm
              </h2>
              <p>
                Trải qua 70 năm đồng hành cùng sự lớn mạnh của Quân đội nhân dân Việt Nam, Trường Sĩ quan Pháo binh đã đào tạo hàng vạn cán bộ chỉ huy, cán bộ kỹ thuật và cán bộ chính trị cho các đơn vị pháo binh trên khắp các chiến trường.
              </p>
              <p>
                Phương châm <em>"Nhà trường gắn liền với chiến trường, lý thuyết đi đôi với thực hành"</em> luôn là sợi chỉ đỏ xuyên suốt qua mọi giai đoạn lịch sử.
              </p>
            </div>
          )}

          {/* TAB NHÂN VẬT TIÊU BIỂU */}
          {activeTab === "figures" && (
            <div className="bg-white p-6 rounded-lg shadow-sm border border-slate-200">
              <h2 className="text-xl font-bold uppercase text-red-900 border-b-2 border-red-800 pb-2 mb-4">
                Nhân vật & Tấm gương tiêu biểu
              </h2>
              <div className="space-y-4 text-sm text-slate-700">
                <div className="p-4 bg-slate-50 rounded border border-slate-200">
                  <h4 className="font-bold text-red-900">Các thế hệ Chỉ huy, Giảng viên Nhà giáo Nhân dân, Nhà giáo Ưu tú</h4>
                  <p className="text-xs text-slate-600 mt-1">Những người thầy mang quân hàm đã tận tụy truyền thụ kinh nghiệm chiến đấu và kiến thức khoa học quân sự cho các thế hệ học viên.</p>
                </div>
                <div className="p-4 bg-slate-50 rounded border border-slate-200">
                  <h4 className="font-bold text-red-900">Các Anh hùng LLVTND xuất thân từ Nhà trường</h4>
                  <p className="text-xs text-slate-600 mt-1">Những tấm gương quả cảm, mưu trí trên các mặt trận chống giặc cứu nước và bảo vệ biên giới Tổ quốc.</p>
                </div>
              </div>
            </div>
          )}

        </div>

        {/* CỘT PHẢI: WIDGET THÔNG TIN & TRUY CẬP ĐIỆN TỬ */}
        <div className="space-y-6">
          
          {/* Card Mã QR & Tên miền */}
          <div className="bg-gradient-to-br from-red-900 to-red-950 text-white p-5 rounded-lg shadow-md border-t-4 border-yellow-400 text-center">
            <h3 className="font-bold text-base uppercase text-yellow-300 mb-1">Cổng điện tử Kỷ niệm</h3>
            <p className="text-xs text-slate-200 font-mono mb-3">{websiteUrl}</p>
            <div className="bg-white p-3 rounded-lg inline-block shadow">
              <QRCodeSVG value={websiteUrl} size={130} />
            </div>
            <p className="text-[11px] text-slate-300 mt-3 leading-tight">
              Quét mã bằng điện thoại hoặc máy tính bảng để truy cập cổng thông tin mọi lúc mọi nơi.
            </p>
          </div>

          {/* Sứ mạng Nhà trường */}
          <div className="bg-white p-5 rounded-lg shadow-sm border border-slate-200">
            <h4 className="font-bold text-red-900 text-sm uppercase border-b pb-2 mb-3">Sứ mạng Nhà trường</h4>
            <p className="text-xs text-slate-600 leading-relaxed">
              Đào tạo sĩ quan chỉ huy phân đội trình độ đại học chuyên ngành pháo binh; bồi dưỡng, nâng cao năng lực cho đội ngũ cán bộ, sẵn sàng đáp ứng yêu cầu tác chiến trong chiến tranh hiện đại công nghệ cao.
            </p>
          </div>

          {/* Liên hệ hành chính */}
          <div className="bg-white p-5 rounded-lg shadow-sm border border-slate-200 text-xs text-slate-600 space-y-2">
            <h4 className="font-bold text-red-900 text-sm uppercase border-b pb-2 mb-3">Thông tin liên hệ</h4>
            <p><strong>Cơ quan:</strong> Trường Sĩ quan Pháo binh</p>
            <p><strong>Địa chỉ:</strong> Xã Thanh Mỹ, TX. Sơn Tây, TP. Hà Nội</p>
            <p><strong>Điện thoại:</strong> 02433.930.194 - 069.595.831</p>
            <p><strong>Cổng chính thức:</strong> <a href="https://tsqpb.edu.vn" target="_blank" className="text-red-700 underline">tsqpb.edu.vn</a></p>
          </div>

        </div>

      </main>

      {/* POP-UP MODAL QR CODE */}
      {showQR && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-sm w-full p-6 text-center shadow-2xl relative">
            <button
              onClick={() => setShowQR(false)}
              className="absolute top-3 right-3 text-slate-400 hover:text-slate-700"
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="font-bold text-lg text-red-900">MÃ QR TRUY CẬP WEBSITE</h3>
            <p className="text-xs text-slate-500 mt-1 mb-4">
              Kỷ niệm 70 năm Trường Sĩ quan Pháo binh Anh hùng
            </p>
            <div className="flex justify-center p-4 bg-slate-50 rounded-lg border border-slate-200 mx-auto w-fit">
              <QRCodeSVG value={websiteUrl} size={180} />
            </div>
            <p className="mt-3 text-xs font-mono font-bold text-red-800 break-all bg-red-50 py-1.5 px-2 rounded">
              {websiteUrl}
            </p>
          </div>
        </div>
      )}

      {/* FOOTER CỔNG THÔNG TIN */}
      <footer className="bg-red-950 text-slate-400 text-xs py-8 border-t-2 border-yellow-500 mt-auto">
        <div className="max-w-7xl mx-auto px-4 text-center space-y-2">
          <p className="text-white font-bold text-sm tracking-wide">
            BẢN QUYỀN THUỘC TRƯỜNG SĨ QUAN PHÁO BINH - BỘ QUỐC PHÒNG
          </p>
          <p>Trang thông tin Kỷ niệm 70 năm Ngày thành lập và đón nhận phần thưởng cao quý</p>
          <p className="text-slate-500 font-mono">Đường dẫn truy cập: https://70nam_tsqpb.edu.vn</p>
        </div>
      </footer>
    </div>
  );
}