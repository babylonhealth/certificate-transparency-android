import com.appmattus.markdown.dsl.markdownLintConfig
import com.appmattus.markdown.rules.LineLengthRule

markdownLintConfig {
    rules {
        +LineLengthRule(codeBlocks = false)
    }
}
