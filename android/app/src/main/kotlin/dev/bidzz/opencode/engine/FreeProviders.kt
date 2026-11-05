package dev.bidzz.opencode.engine

/**
 * Free LLM providers from awesome-freellm-apis.
 * These are OpenAI-compatible endpoints that don't require a credit card.
 * Each provider has a base URL, optional API key requirement, and model list.
 */
data class FreeProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val requiresKey: Boolean,
    val models: List<FreeModel>,
)

data class FreeModel(
    val id: String,
    val name: String,
    val contextLength: Int = 128_000,
)

object FreeProviders {

    val all = listOf(
        FreeProvider(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("nvidia/nemotron-3-ultra-550b-a55b:free", "Nemotron 3 Ultra (free)", 1_000_000),
                FreeModel("minimax/minimax-m3.1-flash:free", "MiniMax M3.1 Flash (free)", 1_000_000),
                FreeModel("nvidia/nemotron-3.5-super-vl:free", "Nemotron 3.5 Super VL (free)", 1_000_000),
                FreeModel("nvidia/nemotron-3.5-lightning:free", "Nemotron 3.5 Lightning (free)", 1_000_000),
                FreeModel("poolside/laguna-s-2.1:free", "Laguna S 2.1 (free)", 262_000),
                FreeModel("poolside/laguna-m-1:free", "Laguna M.1 (free)", 262_000),
                FreeModel("nvidia/nemotron-3-super-120b-a12b:free", "Nemotron 3 Super (free)", 262_000),
                FreeModel("dots-studio/dots-3-note-preview:free", "Dots3-Note Preview (free)", 512_000),
                FreeModel("apodex/apodex-1.1-mini:free", "Apodex 1.1 Mini (free)", 262_000),
            ),
        ),
        FreeProvider(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("llama-3.3-70b-versatile", "Llama 3.3 70B", 131_072),
                FreeModel("llama-3.1-8b-instant", "Llama 3.1 8B Instant", 131_072),
                FreeModel("llama3-70b-8192", "Llama 3 70B", 8_192),
                FreeModel("llama3-8b-8192", "Llama 3 8B", 8_192),
                FreeModel("mixtral-8x7b-32768", "Mixtral 8x7B", 32_768),
                FreeModel("gemma2-9b-it", "Gemma 2 9B IT", 8_192),
                FreeModel("moonshotai/kimi-k2-instruct", "Kimi K2", 131_072),
                FreeModel("moonshotai/kimi-k2-instruct-0905", "Kimi K2 0905", 131_072),
            ),
        ),
        FreeProvider(
            id = "gemini",
            name = "Google Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            requiresKey = true,
            models = listOf(
                FreeModel("gemini-2.0-flash", "Gemini 2.0 Flash", 1_000_000),
                FreeModel("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", 1_000_000),
                FreeModel("gemini-1.5-flash", "Gemini 1.5 Flash", 1_000_000),
                FreeModel("gemini-1.5-flash-8b", "Gemini 1.5 Flash 8B", 1_000_000),
                FreeModel("gemini-1.5-pro", "Gemini 1.5 Pro", 2_000_000),
            ),
        ),
        FreeProvider(
            id = "mistral",
            name = "Mistral AI",
            baseUrl = "https://api.mistral.ai/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("mistral-medium-3.5-128b", "Mistral Medium 3.5", 256_000),
                FreeModel("open-mistral-7b", "Mistral 7B", 32_000),
                FreeModel("open-mixtral-8x7b", "Mixtral 8x7B", 32_000),
                FreeModel("open-mixtral-8x22b", "Mixtral 8x22B", 64_000),
            ),
        ),
        FreeProvider(
            id = "llm7",
            name = "LLM7.io",
            baseUrl = "https://api.llm7.io/v1",
            requiresKey = false,
            models = listOf(
                FreeModel("gpt-oss-20b", "GPT OSS 20B", 128_000),
                FreeModel("mistral-Nemo-Instruct-2407", "Mistral Nemo", 128_000),
                FreeModel("minimax-m2.7", "MiniMax M2.7", 180_000),
            ),
        ),
        FreeProvider(
            id = "kilo",
            name = "Kilo Code",
            baseUrl = "https://api.kilo.ai/api/gateway",
            requiresKey = false,
            models = listOf(
                FreeModel("nvidia/nemotron-3-ultra-550b-a55b:free", "Nemotron 3 Ultra (free)", 1_000_000),
                FreeModel("stepfun/step-3.7-flash:free", "Step 3.7 Flash (free)", 262_000),
                FreeModel("nvidia/nemotron-3-super-120b-a12b:free", "Nemotron 3 Super (free)", 262_000),
            ),
        ),
        FreeProvider(
            id = "huggingface",
            name = "Hugging Face",
            baseUrl = "https://router.huggingface.co/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("meta-llama-3-1-8b-instruct", "Llama 3.1 8B", 128_000),
                FreeModel("google/gemma-3-4b-it", "Gemma 3 4B IT", 131_000),
                FreeModel("phi-4", "Phi 4", 16_000),
            ),
        ),
        FreeProvider(
            id = "zai",
            name = "Z AI (Zhipu)",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            requiresKey = true,
            models = listOf(
                FreeModel("glm-4.7-flash", "GLM-4.7 Flash", 200_000),
                FreeModel("glm-4.5-flash", "GLM-4.5 Flash", 128_000),
                FreeModel("glm-4.6v-flash", "GLM-4.6V Flash", 128_000),
            ),
        ),
        FreeProvider(
            id = "cerebras",
            name = "Cerebras",
            baseUrl = "https://api.cerebras.ai/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("llama3.1-70b", "Llama 3.1 70B", 131_072),
                FreeModel("llama3.1-8b", "Llama 3.1 8B", 131_072),
            ),
        ),
        FreeProvider(
            id = "cloudflare",
            name = "Cloudflare Workers AI",
            baseUrl = "https://api.cloudflare.com/client/v4/accounts/{account_id}/ai/run",
            requiresKey = true,
            models = listOf(
                FreeModel("@cf/meta/llama-3.3-70b-instruct-fp8-fast", "Llama 3.3 70B FP8", 24_000),
                FreeModel("@cf/mistral/mistral-7b-instruct-v0.1", "Mistral 7B", 32_000),
                FreeModel("@cf/qwen/qwen1.5-7b-chat", "Qwen 1.5 7B", 32_000),
            ),
        ),
        FreeProvider(
            id = "cohere",
            name = "Cohere",
            baseUrl = "https://api.cohere.com/v2",
            requiresKey = true,
            models = listOf(
                FreeModel("command-a-218b", "Command A+ 218B", 128_000),
                FreeModel("command-a-111b", "Command A 111B", 256_000),
                FreeModel("command-r", "Command R+", 128_000),
            ),
        ),
        FreeProvider(
            id = "opencode",
            name = "OpenCode",
            baseUrl = "https://opencode.ai/inference/openai/v1",
            requiresKey = true,
            models = listOf(
                // Free models (limited time, per opencode.ai/docs/zen)
                FreeModel("big-pickle", "Big Pickle (free)", 200_000),
                FreeModel("crouton-alpha-free", "Crouton Alpha (free)", 200_000),
                FreeModel("pulse-alpha-free", "Pulse Alpha (free)", 200_000),
                FreeModel("glm-5.3-flash-free", "GLM-5.3 Flash (free)", 200_000),
                FreeModel("deepseek-v4.1-flash-free", "DeepSeek V4.1 Flash (free)", 1_000_000),
                FreeModel("beam-free", "Beam (free)", 200_000),
                FreeModel("flint-40b-free", "Flint 40B (free)", 200_000),
                FreeModel("mimo-v2.6-flash-free", "MiMo V2.6 Flash (free)", 1_000_000),
                FreeModel("nemotron-3-ultra-free", "Nemotron 3 Ultra (free)", 1_000_000),
                FreeModel("nemotron-3.5-lightning-free", "Nemotron 3.5 Lightning (free)", 1_000_000),
                FreeModel("nemotron-3.5-super-vl-free", "Nemotron 3.5 Super VL (free)", 1_000_000),
                FreeModel("jev-1.13-free", "Jev 1.13 (free)", 128_000),
                // Pay-as-you-go models
                FreeModel("deepseek-v4.1-flash", "DeepSeek V4.1 Flash", 1_000_000),
                FreeModel("minimax-m3", "MiniMax M3", 1_000_000),
                FreeModel("glm-5.3-flash", "GLM-5.3 Flash", 200_000),
                FreeModel("kimi-k2.6", "Kimi K2.6", 256_000),
                FreeModel("qwen3.8-max", "Qwen3.8 Max", 256_000),
                FreeModel("mistral-large-4", "Mistral Large 4", 256_000),
                FreeModel("gpt-5.1", "GPT-5.1", 400_000),
                FreeModel("claude-sonnet-4-5", "Claude Sonnet 4.5", 200_000),
                FreeModel("gemini-3-flash", "Gemini 3 Flash", 1_000_000),
                FreeModel("grok-4.5", "Grok 4.5", 256_000),
            ),
        ),
        FreeProvider(
            id = "siliconflow",
            name = "SiliconFlow",
            baseUrl = "https://api.siliconflow.cn/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("Qwen/Qwen3-8B", "Qwen 3 8B", 128_000),
                FreeModel("deepseek-ai/DeepSeek-R1-Distill-Qwen-7B", "DeepSeek R1 Distill 7B", 131_000),
            ),
        ),
        FreeProvider(
            id = "sambanova",
            name = "SambaNova",
            baseUrl = "https://api.sambanova.ai/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("deepseek-v3-1", "DeepSeek V3.1", 128_000),
                FreeModel("deepseek-v3-2-preview", "DeepSeek V3.2 Preview", 128_000),
                FreeModel("minimax-m2-7", "MiniMax M2.7", 128_000),
            ),
        ),
        FreeProvider(
            id = "xai",
            name = "xAI",
            baseUrl = "https://api.x.ai/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("grok-4.3", "Grok 4.3", 1_000_000),
                FreeModel("grok-4.1-fast", "Grok 4.1 Fast", 2_000_000),
                FreeModel("grok-3-mini", "Grok 3 Mini", 131_072),
            ),
        ),
        FreeProvider(
            id = "ai21",
            name = "AI21 Labs",
            baseUrl = "https://api.ai21.com/studio/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("jamba-large-1-7", "Jamba Large 1.7", 256_000),
                FreeModel("jamba-mini-2", "Jamba Mini 2", 256_000),
            ),
        ),
        FreeProvider(
            id = "deepseek",
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("deepseek-chat", "DeepSeek Chat V3.2", 128_000),
                FreeModel("deepseek-reasoner", "DeepSeek Reasoner R1", 128_000),
            ),
        ),
        FreeProvider(
            id = "nscale",
            name = "Nscale",
            baseUrl = "https://inference.api.nscale.com/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("llama-3-3-70b-instruct", "Llama 3.3 70B", 128_000),
                FreeModel("deepseek-r1-distill-llama-70b", "DeepSeek R1 Distill 70B", 128_000),
            ),
        ),
        FreeProvider(
            id = "nebius",
            name = "Nebius",
            baseUrl = "https://api.studio.nebius.com/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("qwen3-235b-a22b", "Qwen 3 235B", 128_000),
            ),
        ),
        FreeProvider(
            id = "modelscope",
            name = "ModelScope",
            baseUrl = "https://api-inference.modelscope.cn/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("MiniMax/MiniMax-M2.5", "MiniMax M2.5", 204_000),
                FreeModel("qwen-qwen3-5-35b-a3b", "Qwen 3.5 35B", 256_000),
                FreeModel("qwen-qwen3-5-27b", "Qwen 3.5 27B", 256_000),
            ),
        ),
        FreeProvider(
            id = "ovhcloud",
            name = "OVHcloud AI",
            baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("qwen3.5-397b-a17b", "Qwen 3.5 397B", 262_000),
                FreeModel("meta-llama-3_3-70b-instruct", "Llama 3.3 70B", 131_000),
                FreeModel("qwen3.8-27b", "Qwen 3.8 27B", 262_000),
            ),
        ),
        FreeProvider(
            id = "aion",
            name = "Aion Labs",
            baseUrl = "https://api.aionlabs.ai/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("aion-labs-aion-2.0", "Aion 2.0", 128_000),
                FreeModel("aion-labs-aion-rp-llama-3.1-8b", "Aion RP Llama 3.1 8B", 32_000),
                FreeModel("aion-labs-aion-3.0", "Aion 3.0", 128_000),
            ),
        ),
        FreeProvider(
            id = "agnes",
            name = "Agnes AI",
            baseUrl = "https://apihub.agnes-ai.com/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("agnes-1.5-flash", "Agnes 1.5 Flash", 256_000),
                FreeModel("agnes-2.0-flash", "Agnes 2.0 Flash", 256_000),
                FreeModel("agnes-image-2.0-flash", "Agnes Image 2.0 Flash", 4_000),
            ),
        ),
        FreeProvider(
            id = "alibaba",
            name = "Alibaba Cloud",
            baseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("qwen3-max", "Qwen 3 Max", 128_000),
                FreeModel("qwen3-plus", "Qwen 3 Plus", 1_000_000),
                FreeModel("qwen3-vl-plus", "Qwen 3 VL Plus", 128_000),
            ),
        ),
        FreeProvider(
            id = "chutes",
            name = "Chutes.ai",
            baseUrl = "https://api.chutes.ai/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("deepseek-ai/DeepSeek-R1", "DeepSeek R1", 131_000),
                FreeModel("meta-llama/Meta-Llama-3.1-70B-Instruct", "Llama 3.1 70B", 131_000),
            ),
        ),
        FreeProvider(
            id = "glhf",
            name = "Glhf.chat",
            baseUrl = "https://glhf.chat/api/openai/v1",
            requiresKey = true,
            models = listOf(
                FreeModel("meta-llama/Meta-Llama-3.1-70B-Instruct", "Llama 3.1 70B", 131_000),
                FreeModel("mistralai/Mixtral-8x7B-Instruct-v0.1", "Mixtral 8x7B", 32_000),
            ),
        ),
    )

    fun byId(id: String): FreeProvider? = all.find { it.id == id }
}
