package org.ice1000.devkt.ui.swing

import charlie.gensokyo.show
import com.bennyhuo.kotlin.opd.delegateOf
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import org.ice1000.devkt.LaunchInfo
import org.ice1000.devkt.config.GlobalSettings
import org.ice1000.devkt.openapi.util.CompletionElement
import org.ice1000.devkt.openapi.util.CompletionPopup
import org.ice1000.devkt.ui.*
import org.ice1000.devkt.ui.swing.dialogs.ConfigurationImpl
import org.ice1000.devkt.ui.swing.dialogs.PsiViewerImpl
import java.awt.*
import java.awt.event.*
import java.io.File
import java.net.URL
import javax.swing.*
import javax.swing.text.AttributeSet

abstract class AbstractUI(protected val frame: DevKtFrame) : UIBase<AttributeSet>() {
	lateinit var mainPanel: JPanel
	var messageLabel = JLabel()
	protected var menuBar = JMenuBar()
	protected var editor = JTextPane()
	protected var lineNumberLabel = JLabel()
	private var scrollPane = JScrollPane()
	protected var memoryIndicator = JButton()
	var lastPopup: CompletionPopup? = null

	init {
		mainPanel = object : JPanel() {
			public override fun paintComponent(g: Graphics) {
				super.paintComponent(g)
				if (LaunchInfo.noBg) return
				val image = GlobalSettings.backgroundImage.second
				if (null != image) g.drawImage(imageCache ?: image
						.getScaledInstance(mainPanel.width, mainPanel.height, Image.SCALE_SMOOTH)
						.also { imageCache = it }, 0, 0, null)
				g.color = backgroundColorCache ?: Color.decode(GlobalSettings.colorBackground)
						.run { Color(red, green, blue, GlobalSettings.backgroundAlpha) }
						.also { backgroundColorCache = it }
				g.fillRect(0, 0, mainPanel.width, mainPanel.height)
				refreshMemoryIndicator()
			}
		}
		mainPanel.layout = GridLayoutManager(3, 1, Insets(0, 0, 0, 0), -1, -1)
		mainPanel.preferredSize = Dimension(800, 600)
		menuBar.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
		mainPanel.add(menuBar, GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false))
		scrollPane.isOpaque = false
		scrollPane.putClientProperty("html.disable", java.lang.Boolean.TRUE)
		mainPanel.add(scrollPane, GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false))
		val panel1 = JPanel()
		panel1.layout = BorderLayout(0, 0)
		panel1.isDoubleBuffered = false
		panel1.isOpaque = false
		panel1.putClientProperty("html.disable", java.lang.Boolean.TRUE)
		scrollPane.setViewportView(panel1)
		editor.dragEnabled = true
		editor.isOpaque = false
		panel1.add(editor, BorderLayout.CENTER)
		lineNumberLabel.isFocusable = false
		lineNumberLabel.horizontalAlignment = 4
		lineNumberLabel.inheritsPopupMenu = false
		lineNumberLabel.isOpaque = false
		lineNumberLabel.isRequestFocusEnabled = false
		lineNumberLabel.verticalAlignment = 1
		lineNumberLabel.verticalTextPosition = 1
		panel1.add(lineNumberLabel, BorderLayout.WEST)
		val panel2 = JPanel()
		panel2.layout = GridLayoutManager(1, 2, Insets(0, 0, 0, 0), -1, -1)
		mainPanel.add(panel2, GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false))
		messageLabel.text = ""
		messageLabel.putClientProperty("html.disable", true)
		panel2.add(messageLabel, GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false))
		memoryIndicator.isFocusable = true
		memoryIndicator.isOpaque = false
		panel2.add(memoryIndicator, GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, 1, null, null, null, 0, false))
		lineNumberLabel.labelFor = editor
		frame.jMenuBar = menuBar
		with(scrollPane) {
			viewport.isOpaque = false
			verticalScrollBar.unitIncrement = 16
		}

		memoryIndicator.addMouseListener(object : MouseAdapter() {
			override fun mouseClicked(e: MouseEvent?) {
				System.gc()
				refreshMemoryIndicator()
			}
		})

		editor.addKeyListener(object : KeyAdapter() {
			override fun keyTyped(e: KeyEvent?) {
				lastPopup?.hide()
			}
		})
	}

	var imageCache: Image? = null
	var backgroundColorCache: Color? = null

	override var memoryIndicatorText by delegateOf(memoryIndicator::getText, memoryIndicator::setText)

	override fun message(text: String) {
		messageLabel.text = text
	}

	override fun showCompletionPopup(completionList: Collection<CompletionElement>): CompletionPopup {
		lastPopup?.hide()
		val point = editor.ui.modelToView(editor, editor.caret.dot)
		val windowPoint = editor.locationOnScreen
		val jList = JList(completionList
				.map { it.lookup }
				.toTypedArray())
		jList.addKeyListener(object : KeyAdapter() {
			override fun keyPressed(e: KeyEvent) {
				when (e.keyCode) {
					KeyEvent.VK_ESCAPE,
					KeyEvent.VK_CONTROL,
					KeyEvent.VK_SHIFT,
					KeyEvent.VK_ALT -> lastPopup?.hide()
					KeyEvent.VK_UP,
					KeyEvent.VK_DOWN,
					KeyEvent.VK_LEFT,
					KeyEvent.VK_RIGHT -> return
					KeyEvent.VK_ENTER -> TODO("select current")
					KeyEvent.VK_TAB -> TODO("select current and replace")
					KeyEvent.VK_BACK_SPACE -> document.delete(document.document.caretPosition - 1, 1)
					else -> document.handleInsert(document.document.caretPosition, e.keyChar.toString())
				}
			}
		})

		val jScrollPane = JScrollPane(jList)
		return PopupFactory.getSharedInstance()
				.getPopup(editor, jScrollPane, windowPoint.x + point.x, windowPoint.y + point.y + 20)
				.let { SwingPopup(it, jList) }
				.also { lastPopup = it }
	}

	override fun dialog(text: String, messageType: MessageType, title: String) {
		JOptionPane.showMessageDialog(mainPanel, text, title, when (messageType) {
			MessageType.Error -> JOptionPane.ERROR_MESSAGE
			MessageType.Information -> JOptionPane.INFORMATION_MESSAGE
			MessageType.Plain -> JOptionPane.PLAIN_MESSAGE
			MessageType.Question -> JOptionPane.QUESTION_MESSAGE
			MessageType.Warning -> JOptionPane.WARNING_MESSAGE
		})
	}

	override fun dialogYesNo(text: String, messageType: MessageType, title: String) =
			JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(mainPanel, text, title, JOptionPane.YES_NO_OPTION, when (messageType) {
				MessageType.Error -> JOptionPane.ERROR_MESSAGE
				MessageType.Information -> JOptionPane.INFORMATION_MESSAGE
				MessageType.Plain -> JOptionPane.PLAIN_MESSAGE
				MessageType.Question -> JOptionPane.QUESTION_MESSAGE
				MessageType.Warning -> JOptionPane.WARNING_MESSAGE
			})

	override fun chooseFile(
			from: File?, chooseFileType: ChooseFileType): File? =
			jFileChooser(from, chooseFileType, JFileChooser.FILES_ONLY).selectedFile

	override fun chooseDir(
			from: File?, chooseFileType: ChooseFileType): File? =
			jFileChooser(from, chooseFileType, JFileChooser.DIRECTORIES_ONLY).currentDirectory

	private fun jFileChooser(from: File?, chooseFileType: ChooseFileType, selectionMode: Int): JFileChooser {
		return JFileChooser(from).apply {
			dialogType = when (chooseFileType) {
				ChooseFileType.Open -> JFileChooser.OPEN_DIALOG
				ChooseFileType.Save,
				ChooseFileType.Create -> JFileChooser.SAVE_DIALOG
			}
			fileSelectionMode = selectionMode
			showDialog(mainPanel, null)
		}
	}

	override fun doBrowse(url: String) = Desktop.getDesktop().browse(URL(url).toURI())
	override fun doOpen(file: File) = Desktop.getDesktop().open(file)
	override fun uiThread(lambda: () -> Unit) = SwingUtilities.invokeLater(lambda)
	override fun dispose() = frame.dispose()
	override fun createSelf() {
		DevKtFrame()
	}

	fun settings() {
		ConfigurationImpl(this, frame).show
	}

	fun viewPsi() {
		psiFile()?.let { PsiViewerImpl(it, frame).show }
	}
}
