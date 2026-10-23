"use client";

import React, { useState } from "react";
import { 
  Trophy, 
  HelpCircle, 
  Image as ImageIcon, 
  Compass, 
  CheckCircle2, 
  XCircle, 
  RotateCcw, 
  ArrowRight, 
  Award,
  ShieldCheck,
  Star
} from "lucide-react";

// 1. DỮ LIỆU CÂU HỎI CHO CÁC CHẾ ĐỘ CHƠI
const GAME_DATA = {
  // Chế độ 1: Trắc nghiệm mốc son lịch sử
  milestones: [
    {
      id: 1,
      question: "Ngày truyền thống của Trường Sĩ quan Pháo binh là ngày nào?",
      options: ["18/11/1946", "18/02/1957", "13/04/1967", "22/12/1944"],
      correct: 1,
      explanation: "Ngày 18/02/1957, Bộ trưởng Bộ Quốc phòng kiêm Tổng Tư lệnh ký ban hành Nghị định số 32 NĐ-A thành lập Trường Sĩ quan Pháo binh."
    },
    {
      id: 2,
      question: "Địa điểm đóng quân hiện nay của Trường Sĩ quan Pháo binh ở đâu?",
      options: ["TX. Sơn Tây, TP. Hà Nội", "Huyện Gia Lâm, TP. Hà Nội", "Huyện Lục Nam, Tỉnh Bắc Giang", "TP. Thái Nguyên"],
      correct: 0,
      explanation: "Trường Sĩ quan Pháo binh đóng quân tại xã Thanh Mỹ, thị xã Sơn Tây, thành phố Hà Nội."
    }
  ],

  // Chế độ 2: Giải mã khẩu hiệu, giá trị truyền thống
  tradition: [
    {
      id: 1,
      question: "Chủ tịch Hồ Chí Minh đã khen tặng Bộ đội Pháo binh 8 chữ vàng truyền thống nào?",
      options: [
        "Trung với Đảng, hiếu với dân",
        "Chân đồng vai sắt, đánh giỏi, bắn trúng",
        "Đoàn kết, kỷ luật, mưu trí, sáng tạo",
        "Chủ động, kiên cường, toàn thắng"
      ],
      correct: 1,
      explanation: "Ngày 13/4/1967, trong thư khen bộ đội pháo binh, Bác Hồ đã khen tặng 8 chữ vàng: 'Chân đồng vai sắt, đánh giỏi, bắn trúng'."
    },
    {
      id: 2,
      question: "Phương châm giáo dục - đào tạo xuyên suốt của Nhà trường là gì?",
      options: [
        "Học đi đôi với hành, lý luận gắn liền thực tiễn",
        "Chất lượng đào tạo của nhà trường là khả năng sẵn sàng chiến đấu của đơn vị",
        "Nhà trường gắn liền với chiến trường",
        "Cả B và C đều đúng"
      ],
      correct: 3,
      explanation: "Nhà trường luôn quán triệt phương châm 'Chất lượng đào tạo của nhà trường là khả năng sẵn sàng chiến đấu của đơn vị' và 'Nhà trường gắn liền với chiến trường'."
    }
  ],

  // Chế độ 3: Nhìn hình ảnh đoán sự kiện / nhân vật
  guessImage: [
    {
      id: 1,
      imageUrl: "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=600&auto=format&fit=crop&q=60", // Thay bằng ảnh tư liệu
      hint: "Khẩu pháo lựu 105mm kéo vào trận địa trong chiến dịch lịch sử nào?",
      question: "Bức ảnh gắn liền với chiến công lịch sử nào của quân đội ta?",
      options: ["Chiến dịch Điện Biên Phủ (1954)", "Chiến dịch Tây Nguyên (1975)", "Chiến dịch Hồ Chí Minh (1975)", "Chiến dịch Biên giới (1950)"],
      correct: 0,
      explanation: "Lực lượng pháo binh đã kéo pháo bằng tay vượt đèo dốc vào trận địa Điện Biên Phủ, giáng đòn quyết định vào tập đoàn cứ điểm của địch."
    }
  ],

  // Chế độ 4: Trò chơi Chinh phục hành trình 70 năm
  journey70: [
    {
      id: 1,
      stage: "Chặng 1: Mở đầu vẻ vang (1957)",
      question: "Văn bản nào chính thức đánh dấu sự ra đời của Trường Sĩ quan Pháo binh?",
      options: [
        "Sắc lệnh số 234/SL",
        "Nghị định số 32 NĐ-A của Bộ Quốc phòng",
        "Chỉ thị số 12/CT của Ban Bí thư",
        "Quyết định số 56/QĐ-BQP"
      ],
      correct: 1,
      explanation: "Nghị định 32 NĐ-A ngày 18/02/1957 đã chính thức thành lập trường để đào tạo cán bộ chỉ huy chính quy."
    },
    {
      id: 2,
      stage: "Chặng 2: Vinh quang 70 năm",
      question: "Trường Sĩ quan Pháo binh đã vinh dự đón nhận danh hiệu cao quý nào?",
      options: [
        "Anh hùng Lực lượng vũ trang nhân dân",
        "Huân chương Sao vàng",
        "Huân chương Hồ Chí Minh",
        "Tất cả các danh hiệu trên"
      ],
      correct: 3,
      explanation: "Nhà trường tự hào được Đảng và Nhà nước trao tặng 02 lần danh hiệu Anh hùng LLVTND cùng nhiều Huân chương cao quý."
    }
  ]
};

export default function HistoryGameSystem() {
  const [activeGame, setActiveGame] = useState<"milestones" | "tradition" | "guessImage" | "journey70">("milestones");
  const [currentIndex, setCurrentIndex] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
  const [isAnswered, setIsAnswered] = useState(false);
  const [score, setScore] = useState(0);
  const [showResult, setShowResult] = useState(false);

  const currentQuestions = GAME_DATA[activeGame];
  const q = currentQuestions[currentIndex];

  // Xử lý chọn đáp án
  const handleSelectAnswer = (idx: number) => {
    if (isAnswered) return;
    setSelectedAnswer(idx);
    setIsAnswered(true);

    if (idx === q.correct) {
      setScore(prev => prev + 10);
    }
  };

  // Qua câu tiếp theo hoặc kết thúc
  const handleNext = () => {
    if (currentIndex + 1 < currentQuestions.length) {
      setCurrentIndex(prev => prev + 1);
      setSelectedAnswer(null);
      setIsAnswered(false);
    } else {
      setShowResult(true);
    }
  };

  // Reset chơi lại
  const handleRestart = (mode = activeGame) => {
    setActiveGame(mode);
    setCurrentIndex(0);
    setSelectedAnswer(null);
    setIsAnswered(false);
    setScore(0);
    setShowResult(false);
  };

  return (
    <div className="min-h-screen bg-slate-100 text-slate-800 flex flex-col font-sans">
      {/* HEADER QUÂN SỰ TRANG TRỌNG */}
      <header className="bg-red-950 text-white border-b-4 border-yellow-500 py-4 px-6 shadow-md">
        <div className="max-w-5xl mx-auto flex flex-col sm:flex-row justify-between items-center gap-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center p-1 border-2 border-yellow-400">
              <ShieldCheck className="w-6 h-6 text-red-900" />
            </div>
            <div>
              <p className="text-[10px] font-bold uppercase tracking-widest text-yellow-400">
                TRƯỜNG SĨ QUAN PHÁO BINH • HÀNH TRÌNH 70 NĂM
              </p>
              <h1 className="text-base sm:text-lg font-black uppercase text-white">
                Hệ Thống Trò Chơi & Kiểm Tra Kiến Thức Lịch Sử
              </h1>
            </div>
          </div>
          <div className="flex items-center gap-2 bg-red-900/80 px-3 py-1 rounded border border-red-700 text-xs font-semibold text-yellow-300">
            <Trophy className="w-4 h-4 text-yellow-400" />
            Điểm số: {score}
          </div>
        </div>
      </header>

      {/* CHỌN CHẾ ĐỘ CHƠI */}
      <nav className="bg-white border-b border-slate-200">
        <div className="max-w-5xl mx-auto px-4 flex overflow-x-auto gap-2 py-3">
          {[
            { id: "milestones", title: "Mốc son lịch sử", icon: HelpCircle },
            { id: "tradition", title: "Giải mã khẩu hiệu", icon: Award },
            { id: "guessImage", title: "Nhìn hình đoán sự kiện", icon: ImageIcon },
            { id: "journey70", title: "Chinh phục hành trình 70 năm", icon: Compass },
          ].map(tab => {
            const Icon = tab.icon;
            const isActive = activeGame === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => handleRestart(tab.id as any)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs sm:text-sm font-bold whitespace-nowrap transition ${
                  isActive 
                    ? "bg-red-800 text-yellow-300 shadow-sm" 
                    : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                }`}
              >
                <Icon className="w-4 h-4" />
                {tab.title}
              </button>
            );
          })}
        </div>
      </nav>

      {/* KHUNG NỘI DUNG TRÒ CHƠI */}
      <main className="max-w-3xl mx-auto w-full px-4 py-8 flex-1">
        {!showResult ? (
          <div className="bg-white rounded-xl shadow border border-slate-200 p-6 space-y-6">
            {/* Tiến trình câu hỏi */}
            <div className="flex justify-between items-center text-xs font-semibold text-slate-500 border-b pb-3">
              <span>Câu hỏi {currentIndex + 1} / {currentQuestions.length}</span>
              <span className="text-red-800 font-bold">
                {activeGame === "journey70" && (q as any).stage}
              </span>
            </div>

            {/* Khung hiển thị ảnh đối với game nhìn hình */}
            {activeGame === "guessImage" && (
              <div className="space-y-2">
                <div className="w-full h-48 sm:h-64 bg-slate-100 rounded-lg overflow-hidden border border-slate-300 flex items-center justify-center">
                  <img 
                    src={(q as any).imageUrl} 
                    alt="Hình ảnh câu hỏi tư liệu" 
                    className="w-full h-full object-cover"
                  />
                </div>
                <p className="text-xs text-slate-500 italic">💡 Gợi ý: {(q as any).hint}</p>
              </div>
            )}

            {/* Nội dung câu hỏi */}
            <h2 className="text-base sm:text-lg font-bold text-slate-900 leading-snug">
              {q.question}
            </h2>

            {/* Danh sách lựa chọn */}
            <div className="space-y-3">
              {q.options.map((opt, idx) => {
                let btnStyle = "bg-slate-50 border-slate-200 hover:bg-slate-100 text-slate-800";
                
                if (isAnswered) {
                  if (idx === q.correct) {
                    btnStyle = "bg-emerald-50 border-emerald-500 text-emerald-800 font-bold";
                  } else if (idx === selectedAnswer) {
                    btnStyle = "bg-rose-50 border-rose-500 text-rose-800 font-bold";
                  } else {
                    btnStyle = "bg-slate-50 border-slate-200 opacity-60";
                  }
                }

                return (
                  <button
                    key={idx}
                    disabled={isAnswered}
                    onClick={() => handleSelectAnswer(idx)}
                    className={`w-full text-left p-3.5 rounded-lg border-2 text-sm transition flex items-center justify-between ${btnStyle}`}
                  >
                    <span>{opt}</span>
                    {isAnswered && idx === q.correct && (
                      <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
                    )}
                    {isAnswered && idx === selectedAnswer && idx !== q.correct && (
                      <XCircle className="w-5 h-5 text-rose-600 flex-shrink-0" />
                    )}
                  </button>
                );
              })}
            </div>

            {/* Phản hồi sau mỗi câu trả lời */}
            {isAnswered && (
              <div className="mt-4 p-4 rounded-lg bg-amber-50 border-l-4 border-amber-500 text-xs sm:text-sm text-slate-800 space-y-1">
                <p className="font-bold text-amber-900 flex items-center gap-1">
                  <Star className="w-4 h-4 fill-amber-500 text-amber-500" /> Giải thích chi tiết:
                </p>
                <p className="leading-relaxed">{q.explanation}</p>
              </div>
            )}

            {/* Nút tiếp tục */}
            {isAnswered && (
              <div className="flex justify-end pt-2">
                <button
                  onClick={handleNext}
                  className="bg-red-800 hover:bg-red-900 text-white font-bold px-5 py-2.5 rounded-lg text-sm flex items-center gap-1.5 shadow transition"
                >
                  {currentIndex + 1 < currentQuestions.length ? "Câu tiếp theo" : "Xem kết quả"}
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        ) : (
          /* MÀN HÌNH TỔNG KẾT KẾT QUẢ */
          <div className="bg-white rounded-xl shadow border border-slate-200 p-8 text-center space-y-6">
            <div className="w-16 h-16 bg-yellow-100 text-yellow-600 rounded-full flex items-center justify-center mx-auto shadow-inner">
              <Trophy className="w-8 h-8" />
            </div>

            <div className="space-y-1">
              <h2 className="text-2xl font-black uppercase text-red-900">
                Chúc Mừng Bạn Đã Hoàn Thành!
              </h2>
              <p className="text-xs sm:text-sm text-slate-500">
                Đánh giá mức độ am hiểu lịch sử & truyền thống Trường SQ Pháo binh
              </p>
            </div>

            <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 max-w-xs mx-auto">
              <div className="text-3xl font-black text-red-800">{score} điểm</div>
              <p className="text-xs text-slate-600 mt-1">
                Số câu đúng: {score / 10} / {currentQuestions.length} câu
              </p>
            </div>

            <p className="text-sm text-slate-700 italic max-w-md mx-auto">
              {score >= currentQuestions.length * 8
                ? "Xuất sắc! Bạn nắm rất vững truyền thống và các mốc son lịch sử vẻ vang của Nhà trường."
                : "Hãy tiếp tục ôn luyện và khám phá kho tư liệu số để đạt kết quả cao hơn nhé!"}
            </p>

            <button
              onClick={() => handleRestart()}
              className="inline-flex items-center gap-2 bg-red-800 hover:bg-red-900 text-white font-bold px-6 py-2.5 rounded-lg text-sm shadow transition"
            >
              <RotateCcw className="w-4 h-4" />
              Chơi lại chế độ này
            </button>
          </div>
        )}
      </main>

      {/* FOOTER */}
      <footer className="bg-red-950 text-slate-400 text-xs py-4 text-center border-t border-red-900">
        Ban Chỉ đạo Cuộc thi Tìm hiểu 70 năm Ngày truyền thống Trường Sĩ quan Pháo binh
      </footer>
    </div>
  );
}