package com.example.bluetoothprinter

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothState
import hr.istratech.bixolon.driver.charset.ByteCharset
import hr.istratech.bixolon.driver.command.general.Alignment
import hr.istratech.bixolon.driver.command.print.*
import hr.istratech.bixolon.driver.command.qr.QrCodeErrorCorrectionLevel
import hr.istratech.bixolon.driver.command.qr.QrCodeModel
import hr.istratech.bixolon.driver.command.qr.QrCodeSize
import hr.istratech.bixolon.driver.general.Printer
import hr.istratech.bixolon.driver.general.QrPrinterBuilder
import hr.istratech.bixolon.driver.general.TextPrinterBuilder
import java.nio.charset.Charset


/**
 * @author ksaric
 */
class BluetoothBixolonActivity : Activity() {
    private var connectButton: Button? = null
    private var charsetsCheckBox: CheckBox? = null
    private var charsetButton: Button? = null
    private var textEditText: TextView? = null
    private var textPrintButton: Button? = null
    private var textPremutationsPrintButton: Button? = null
    private var qrEditText: TextView? = null
    private var qrPrintButton: Button? = null
    private var qrPermutationsPrintButton: Button? = null
    private var closeButton: Button? = null
    private var bluetoothSPP: BluetoothSPP? = null
    private var userFeedback: UserFeedback? = null
    private var TAG: String? = "BluetoothBixolonActivity"

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_main)
        connectButton = findViewById<View>(R.id.connect_button) as Button
        charsetsCheckBox = findViewById<View>(R.id.charsets_checkbox) as CheckBox
        charsetButton = findViewById<View>(R.id.charset_button) as Button
        textEditText = findViewById<View>(R.id.text_edit_text) as TextView
        textPrintButton = findViewById<View>(R.id.text_print_button) as Button
        textPremutationsPrintButton = findViewById<View>(R.id.text_permutations_print_button) as Button
        qrEditText = findViewById<View>(R.id.qr_text) as TextView
        qrPrintButton = findViewById<View>(R.id.qr_print_button) as Button
        qrPermutationsPrintButton = findViewById<View>(R.id.qr_premutations_print_button) as Button
        closeButton = findViewById<View>(R.id.close_button) as Button
        init()
    }

    public override fun onStart() {
        super.onStart()
        if (!bluetoothSPP!!.isBluetoothEnabled()) {
            // Do somthing if bluetooth is disabled
        } else {
            // Do something if bluetooth is already enabled
        }
    }

    private fun init() {
        bluetoothSPP = BluetoothSPP(this)
        userFeedback = UserFeedback(this)
        connectButton!!.setOnClickListener { connect() }
        charsetButton!!.setOnClickListener { printCharsets() }
        textPrintButton!!.setOnClickListener { printText() }
        textPremutationsPrintButton!!.setOnClickListener { printTextPermutations() }
        qrPrintButton!!.setOnClickListener { printQr() }
        qrPermutationsPrintButton!!.setOnClickListener { printQrPermutations() }
        closeButton!!.setOnClickListener { bluetoothSPP!!.disconnect() }

        // first pass
        charsetsCheckBox!!.isChecked = false
        charsetsCheckBox!!.setOnClickListener { charsetsCheckBox!!.isChecked = charsetsCheckBox!!.isChecked }
    }

    private fun printCodePageChars(codePage: CodePage): String {
        val stringBuilder = StringBuilder()
        val charset: ByteCharset = codePage.getCharset()
        for (c in charset.getLookupTable()) {
            stringBuilder.append(c)
        }
        stringBuilder.append(NEW_LINE)
        stringBuilder.append(NEW_LINE)
        return stringBuilder.toString()
    }

    private fun printCodePageCharsGeneric(codePage: CodePage): String {
        val stringBuilder = StringBuilder()
        val charset: Charset = codePage.getCharset()
        var i = Character.MIN_VALUE.toInt()
        while (i < Character.MAX_VALUE.toInt()) {
            val s = Character.toString(i.toChar())
            val encoded = s.toByteArray(charset)
            val decoded = String(encoded, charset)
            if (s == decoded) {
                stringBuilder.append(s)
            }
            i++
        }
        stringBuilder.append(NEW_LINE)
        stringBuilder.append(NEW_LINE)
        return stringBuilder.toString()
    }

    private fun connect() {
        if (bluetoothSPP!!.isBluetoothEnabled() && bluetoothSPP!!.isBluetoothAvailable()) {
            if (!bluetoothSPP!!.isServiceAvailable()) {
                bluetoothSPP!!.setupService()
                bluetoothSPP!!.startService(BluetoothState.DEVICE_OTHER)
            }
            if (bluetoothSPP!!.isServiceAvailable()) {
                Log.v(TAG, "+++ Connecting device +++")
                bluetoothSPP!!.setAutoConnectionListener(object : BluetoothSPP.AutoConnectionListener {
                    override fun onAutoConnectionStarted() {
                        Log.v(TAG, "+++ Bluetooth device connected +++")
                    }

                    override fun onNewConnection(name: String, address: String) {
                        Log.v(TAG, "+++ New connection on '$name :: $address' +++")
                    }
                })
                bluetoothSPP!!.autoConnect("SPP-")
                bluetoothSPP!!.setOnDataReceivedListener(object : BluetoothSPP.OnDataReceivedListener {
                    override fun onDataReceived(data: ByteArray?, message: String?) {
                        // Do something when data incoming
                        userFeedback!!.alert(message)
                    }
                })
            }
        } else {
            userFeedback!!.error("Connect and enable bluetooth!")
            return
        }
    }

    private fun printCharsets() {
        val isChecked = charsetsCheckBox!!.isChecked
        if (isChecked) {
            for (codePage in CodePage.values()) {
                val printer: Printer = TextPrinterBuilder
                        .aPrinterBuilder()
                        .withCodePage(codePage)
                        .withGeneralControlSequence(Alignment.LEFT)
                        .withTextControlSequence(CharacterSize.NORMAL)
                        .withTextControlSequence(DeviceFont.DEVICE_FONT_A)
                        .buildPrinter(printCodePageChars(codePage))
                bluetoothSPP!!.send(printer.getCommand(), false)
            }
        } else {
            val codePage: CodePage = CodePage.CP_437_USA
            val printer: Printer = TextPrinterBuilder
                    .aPrinterBuilder()
                    .withCodePage(codePage)
                    .withGeneralControlSequence(Alignment.LEFT)
                    .withTextControlSequence(CharacterSize.NORMAL)
                    .withTextControlSequence(DeviceFont.DEVICE_FONT_A)
                    .buildPrinter(printCodePageChars(codePage))
            bluetoothSPP!!.send(printer.getCommand(), false)
        }
    }

    private fun printTextPermutations() {
        val textTextContent = textEditText!!.text
        for (alignment in Alignment.values()) {
            for (emphasize in Emphasize.values()) {
                for (underline in Underline.values()) {
                    for (reverse in Reverse.values()) {
                        for (characterSize in CharacterSize.values()) {
                            for (deviceFont in DeviceFont.values()) {
                                val printer = TextPrinterBuilder
                                        .aPrinterBuilder()
                                        .withCodePage(CodePage.CP_437_USA)
                                        .withGeneralControlSequence(alignment)
                                        .withTextControlSequence(characterSize)
                                        .withTextControlSequence(deviceFont)
                                        .withTextControlSequence(emphasize)
                                        .withTextControlSequence(underline)
                                        .withTextControlSequence(reverse)
                                        .buildPrinter(textTextContent.toString())
                                bluetoothSPP!!.send(printer.command, false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun printText() {
        val textTextContent = textEditText!!.text
        val printer: Printer = TextPrinterBuilder
                .aPrinterBuilder()
                .withCodePage(CodePage.CP_437_USA)
                .withGeneralControlSequence(Alignment.LEFT)
                .withTextControlSequence(CharacterSize.NORMAL)
                .withTextControlSequence(DeviceFont.DEVICE_FONT_A)
                .buildPrinter(textTextContent.toString())
        bluetoothSPP!!.send(printer.getCommand(), false)
    }

    private fun printQrPermutations() {
        val qrTextContent = qrEditText!!.text
        for (alignment in Alignment.values()) {
            for (qrCodeModel in QrCodeModel.values()) {
                for (qrCodeSize in QrCodeSize.values()) {
                    for (errorCorrectionLevel in QrCodeErrorCorrectionLevel.values()) {
                        val printer: Printer = QrPrinterBuilder
                                .aPrinterBuilder()
                                .withGeneralControlSequence(alignment)
                                .withQrControlSequence(qrCodeModel)
                                .withQrControlSequence(qrCodeSize)
                                .withQrControlSequence(errorCorrectionLevel)
                                .buildPrinter(qrTextContent.toString())
                        bluetoothSPP!!.send(printer.getCommand(), false)
                    }
                }
            }
        }
    }

    private fun printQr() {
        val qrTextContent = qrEditText!!.text
        val printer: Printer = QrPrinterBuilder
                .aPrinterBuilder()
                .withGeneralControlSequence(Alignment.CENTER)
                .withQrControlSequence(QrCodeModel.MODEL2)
                .withQrControlSequence(QrCodeSize.SIZE7)
                .withQrControlSequence(QrCodeErrorCorrectionLevel.L)
                .buildPrinter(qrTextContent.toString())
        bluetoothSPP!!.send(printer.getCommand(), false)
    }

    class UserFeedback(private val context: Context) {
        private val activity: Activity
            private get() = context as Activity

        fun alert(message: String?) {
            if (!activity.isFinishing) showPopup(context, message, "Alert")
        }

        fun success(message: String?) {
            if (!activity.isFinishing) showPopup(context, message, "Success")
        }

        fun error(message: String?) {
            if (!activity.isFinishing) showPopup(context, message, "Error")
        }

        fun longToast(message: String?) {
            if (!activity.isFinishing) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }

        fun shortToast(message: String?) {
            if (!activity.isFinishing) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }

        fun showPopup(context: Context?, alertText: String?, title: String?) {
            val alertDialog = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(alertText)
                    .create()

            // Setting OK Button
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok") { dialog, which -> }

            // Showing Alert Message
            alertDialog.show()
        }
    }

    companion object {
        private const val TAG = "BluetoothBixolonActivity"
        const val NEW_LINE = "\r\n"
    }
}