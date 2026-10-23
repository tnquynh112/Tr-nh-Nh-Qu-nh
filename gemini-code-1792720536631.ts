import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.NEXT_PUBLIC_SUPABASE_URL!,
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
);

const saveScoreToSupabase = async (userName: string, finalScore: number) => {
  await supabase.from('quiz_results').insert([
    {
      user_name: userName || "Học viên ẩn danh",
      game_mode: activeGame,
      score: finalScore,
      total_questions: currentQuestions.length,
      completed_at: new Date().toISOString()
    }
  ]);
};