import com.appmattus.markdown.dsl.markdownlint
import com.appmattus.markdown.rules.LineLengthRule

markdownlint {
    rules {
        +LineLengthRule(codeBlocks = false)
    }
}
